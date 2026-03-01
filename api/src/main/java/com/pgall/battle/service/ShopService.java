package com.pgall.battle.service;

import com.pgall.battle.dto.ShopResponse;
import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.entity.Inventory;
import com.pgall.battle.entity.ShopItem;
import com.pgall.battle.enums.BuffType;
import com.pgall.battle.repository.GameCharacterRepository;
import com.pgall.battle.repository.InventoryRepository;
import com.pgall.battle.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class ShopService {

    private static final ReentrantLock shopLock = new ReentrantLock();
    private static final int MAX_POTIONS = 5;
    private static final int MAX_REFRESH_COST = 500;

    private final GameCharacterRepository characterRepository;
    private final InventoryRepository inventoryRepository;
    private final ShopItemRepository shopItemRepository;

    // 캐릭터별 상점 세션 (인메모리)
    private final Map<Long, ShopSession> sessions = new ConcurrentHashMap<>();

    static class ShopSession {
        List<PotionDef> potions;
        boolean[] sold;
        int refreshCount;
    }

    static class PotionDef {
        String name;
        String description;
        int price;
        BuffType buffType;
        List<String> effectNames;
    }

    private static final List<PotionTemplate> POTION_TEMPLATES = List.of(
            new PotionTemplate("치유 물약", "HP를 회복합니다.", 25, BuffType.HEAL),
            new PotionTemplate("고급 치유 물약", "HP를 대량 회복합니다.", 50, BuffType.GREATER_HEAL),
            new PotionTemplate("크리티컬 물약", "크리티컬 범위가 확장됩니다.", 40, BuffType.CRIT_DOUBLE),
            new PotionTemplate("더블 어택 물약", "공격을 2회 수행합니다.", 50, BuffType.DOUBLE_ATTACK),
            new PotionTemplate("수호의 물약", "1회 공격을 차단합니다.", 30, BuffType.SHIELD),
            new PotionTemplate("화염 부여 물약", "무기에 화염 데미지를 추가합니다.", 35, BuffType.FIRE_ENCHANT),
            new PotionTemplate("빙결 부여 물약", "무기에 빙결 데미지를 추가합니다.", 35, BuffType.ICE_ENCHANT),
            new PotionTemplate("번개 부여 물약", "무기에 번개 데미지를 추가합니다.", 35, BuffType.LIGHTNING_ENCHANT),
            new PotionTemplate("신성 부여 물약", "무기에 신성 데미지를 추가합니다.", 40, BuffType.HOLY_ENCHANT),
            new PotionTemplate("관통 물약", "적의 방어력 일부를 무시합니다.", 45, BuffType.PENETRATION_BOOST),
            new PotionTemplate("재생 물약", "매 라운드 HP를 회복합니다.", 40, BuffType.REGEN_POTION),
            new PotionTemplate("반사 물약", "피격 시 데미지를 반사합니다.", 45, BuffType.REFLECT_POTION),
            new PotionTemplate("정확도 물약", "명중률이 증가합니다.", 25, BuffType.ACCURACY_POTION),
            new PotionTemplate("가속 물약", "행동 속도가 증가합니다.", 45, BuffType.HASTE_POTION),
            new PotionTemplate("철피 물약", "물리 데미지를 감소시킵니다.", 40, BuffType.IRON_SKIN_POTION),
            new PotionTemplate("축복 물약", "모든 능력이 소폭 강화됩니다.", 50, BuffType.BLESS_POTION)
    );

    record PotionTemplate(String name, String description, int basePrice, BuffType buffType) {}

    public ShopResponse getShop(Long characterId) {
        GameCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다."));
        ShopSession session = sessions.computeIfAbsent(characterId, k -> generateSession());
        return buildResponse(session, character);
    }

    @Transactional
    public ShopResponse refresh(Long characterId) {
        shopLock.lock();
        try {
            GameCharacter character = characterRepository.findById(characterId)
                    .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다."));

            ShopSession session = sessions.computeIfAbsent(characterId, k -> generateSession());
            int cost = getRefreshCost(session.refreshCount);

            if (cost > MAX_REFRESH_COST) {
                throw new IllegalStateException("최대 리프레시 비용을 초과했습니다.");
            }
            if (character.getGold() < cost) {
                throw new IllegalStateException("골드 부족 (필요: " + cost + "G, 보유: " + character.getGold() + "G)");
            }

            character.setGold(character.getGold() - cost);
            characterRepository.save(character);

            ShopSession newSession = generateSession();
            newSession.refreshCount = session.refreshCount + 1;
            sessions.put(characterId, newSession);

            return buildResponse(newSession, character);
        } finally {
            shopLock.unlock();
        }
    }

    @Transactional
    public ShopResponse buyItem(Long characterId, int index) {
        shopLock.lock();
        try {
            GameCharacter character = characterRepository.findById(characterId)
                    .orElseThrow(() -> new NoSuchElementException("캐릭터를 찾을 수 없습니다."));

            ShopSession session = sessions.get(characterId);
            if (session == null) throw new IllegalStateException("상점을 먼저 열어주세요.");
            if (index < 0 || index >= session.potions.size()) throw new IllegalArgumentException("잘못된 아이템입니다.");
            if (session.sold[index]) throw new IllegalStateException("이미 매진된 아이템입니다.");

            PotionDef potion = session.potions.get(index);
            if (character.getGold() < potion.price) {
                throw new IllegalStateException("골드 부족 (필요: " + potion.price + "G)");
            }

            character.setGold(character.getGold() - potion.price);
            session.sold[index] = true;

            // ShopItem DB 저장
            ShopItem shopItem = shopItemRepository.save(ShopItem.builder()
                    .name(potion.name)
                    .description(potion.description)
                    .price(potion.price)
                    .buffType(potion.buffType)
                    .buffChance(100)
                    .build());

            // Inventory 추가 + 장착 슬롯 여유 있으면 자동 장착
            boolean autoEquip = inventoryRepository.countByCharacterIdAndEquipped(characterId, true) < 5;
            inventoryRepository.findByCharacterIdAndShopItemId(characterId, shopItem.getId())
                    .ifPresentOrElse(
                            inv -> { inv.setQuantity(inv.getQuantity() + 1); inventoryRepository.save(inv); },
                            () -> inventoryRepository.save(Inventory.builder()
                                    .character(character).shopItem(shopItem).quantity(1).equipped(autoEquip).build())
                    );

            characterRepository.save(character);
            return buildResponse(session, character);
        } finally {
            shopLock.unlock();
        }
    }

    /** 매일 0시: 모든 세션 초기화 → refreshCount=0, 가격 5G로 리셋 */
    public void resetSessions() {
        sessions.clear();
    }

    private ShopSession generateSession() {
        ShopSession session = new ShopSession();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int count = 4 + random.nextInt(3); // 4~6개

        List<PotionTemplate> shuffled = new ArrayList<>(POTION_TEMPLATES);
        Collections.shuffle(shuffled, random);

        session.potions = new ArrayList<>();
        for (int i = 0; i < count && i < shuffled.size(); i++) {
            PotionTemplate t = shuffled.get(i);
            PotionDef def = new PotionDef();
            def.name = t.name();
            def.description = t.description();
            def.price = t.basePrice() + random.nextInt(-5, 11);
            def.buffType = t.buffType();
            def.effectNames = List.of(t.buffType().getKoreanName());
            session.potions.add(def);
        }
        session.sold = new boolean[session.potions.size()];
        session.refreshCount = 0;
        return session;
    }

    private int getRefreshCost(int count) {
        return Math.min(5 + count * 5, MAX_REFRESH_COST);
    }

    private ShopResponse buildResponse(ShopSession session, GameCharacter character) {
        List<ShopResponse.PotionItem> items = new ArrayList<>();
        for (int i = 0; i < session.potions.size(); i++) {
            PotionDef p = session.potions.get(i);
            items.add(ShopResponse.PotionItem.builder()
                    .index(i).name(p.name).description(p.description)
                    .price(p.price).effects(p.effectNames).sold(session.sold[i])
                    .build());
        }
        return ShopResponse.builder()
                .items(items)
                .refreshCost(getRefreshCost(session.refreshCount))
                .refreshCount(session.refreshCount)
                .gold(character.getGold())
                .build();
    }
}
