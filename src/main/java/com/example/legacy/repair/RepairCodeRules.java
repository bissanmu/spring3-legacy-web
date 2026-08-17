package com.example.legacy.repair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class RepairCodeRules {

    private static final List<Rule> SIDE_RULES = sideRules();
    private static final List<Rule> POSITION_RULES = positionRules();
    private static final List<Rule> ACTION_RULES = actionRules();
    private static final List<Rule> CATEGORY_RULES = categoryRules();
    private static final List<String> STRUCTURAL_SIGNALS = structuralSignals();
    private static final List<String> HIGH_SEVERITY_SIGNALS = highSeveritySignals();

    private RepairCodeRules() {
    }

    public static String inferSide(String text) {
        return match(text, SIDE_RULES, "UNKNOWN");
    }

    public static String inferPosition(String text) {
        return match(text, POSITION_RULES, "UNKNOWN");
    }

    public static String inferAction(String text) {
        String explicitAction = inferExplicitAction(text);
        if (!"NOT_SPECIFIED".equals(explicitAction)) {
            return explicitAction;
        }
        return match(text, ACTION_RULES, "NOT_SPECIFIED");
    }

    public static String inferExplicitAction(String text) {
        return actionNameToCode(RepairTextNormalizer.extractTrailingActionName(text));
    }

    public static String inferCategory(String text) {
        return match(text, CATEGORY_RULES, "UNKNOWN");
    }

    public static boolean inferStructural(String text) {
        return containsAny(text, STRUCTURAL_SIGNALS);
    }

    public static String inferSeverity(String text, String actionCode) {
        if (containsAny(text, HIGH_SEVERITY_SIGNALS)) {
            return "HIGH";
        }
        if ("REPLACE".equals(actionCode) || "PANEL_BEATING".equals(actionCode)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static String match(String text, List<Rule> rules, String defaultValue) {
        for (Rule rule : rules) {
            if (rule.pattern.matcher(text).find()) {
                return rule.code;
            }
        }
        return defaultValue;
    }

    private static boolean containsAny(String text, List<String> signals) {
        for (String signal : signals) {
            if (text.contains(signal)) {
                return true;
            }
        }
        return false;
    }

    private static String actionNameToCode(String actionName) {
        if (actionName == null) {
            return "NOT_SPECIFIED";
        }
        String normalized = actionName.replaceAll("\\s+", "").toUpperCase();
        if ("1/2OH".equals(normalized) || "1/3OH".equals(normalized)
                || "1/4OH".equals(normalized) || "오버홀".equals(normalized)) {
            return "OVERHAUL";
        }
        if ("수리".equals(normalized)) {
            return "ADJUST";
        }
        if ("조정".equals(normalized)) {
            return "ADJUST";
        }
        if ("도장".equals(normalized)) {
            return "PAINT";
        }
        if ("탈착".equals(normalized)) {
            return "REMOVE_INSTALL";
        }
        if ("교환".equals(normalized)) {
            return "REPLACE";
        }
        if ("판금".equals(normalized)) {
            return "PANEL_BEATING";
        }
        return "NOT_SPECIFIED";
    }

    private static List<Rule> sideRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule("BOTH", "좌우|양쪽|LEFT\\s*RIGHT|LEFTRIGHT|LH\\s*/\\s*RH|RH\\s*/\\s*LH|\\([^)]*좌[^)]*우[^)]*\\)|\\([^)]*우[^)]*좌[^)]*\\)"));
        rules.add(new Rule("LEFT", "(^|\\W)(LH|LEFT|L/H|L\\.H\\.)(\\W|$)|\\([^)]*좌[^)]*\\)|좌측|왼쪽|운전석|\\bL\\(\\)"));
        rules.add(new Rule("RIGHT", "(^|\\W)(RH|RIGHT|R/H|R\\.H\\.)(\\W|$)|\\([^)]*우[^)]*\\)|우측|오른쪽|조수석|\\bR\\(\\)"));
        return Collections.unmodifiableList(rules);
    }

    private static List<Rule> positionRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule("FRONT", "FRONT|FRT|프론트|프런트|앞|전방|전면|전휀더|본네트|후드|라디에이터|라지에이터|헤드램프|전조등"));
        rules.add(new Rule("REAR", "REAR|RR|BACK|리어|뒤|후방|후면|트렁크|테일|백도어|리야"));
        rules.add(new Rule("SIDE", "SIDE|사이드|도어|쿼터|휀더|펜더|필러|실|스텝|로커"));
        rules.add(new Rule("UNDER", "UNDER|언더|하부|플로어|바닥|머플러|서스펜션|로워|LOWER"));
        rules.add(new Rule("UPPER", "UPPER|상부|루프|천정|ROOF"));
        return Collections.unmodifiableList(rules);
    }

    private static List<Rule> actionRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule("OVERHAUL", "1/2\\s*OH|1/3\\s*OH|1/4\\s*OH|오버홀|OVERHAUL"));
        rules.add(new Rule("REPLACE", "교환|교체|대체|REPLACE|ASSY|어셈블리|컴플리트|COMPLETE"));
        rules.add(new Rule("PAINT", "도장|페인트|PAINT|투톤|스프레이|코팅"));
        rules.add(new Rule("PANEL_BEATING", "판금|BEATING|수정|보수"));
        rules.add(new Rule("REMOVE_INSTALL", "탈착|탈부착|분해|장착|REMOVE|INSTALL|탈거"));
        rules.add(new Rule("ADJUST", "조정|교정|얼라인|ALIGN|수리|점검"));
        return Collections.unmodifiableList(rules);
    }

    private static List<Rule> categoryRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule("BUMPER", "범퍼|BUMPER"));
        rules.add(new Rule("FENDER", "휀더|펜더|FENDER"));
        rules.add(new Rule("DOOR", "도어|DOOR"));
        rules.add(new Rule("HOOD", "후드|본네트|보닛|HOOD|BONNET"));
        rules.add(new Rule("TRUNK_TAILGATE", "트렁크|백도어|테일게이트|TAIL|TRUNK|BACK DOOR"));
        rules.add(new Rule("QUARTER_PANEL", "쿼터|QUARTER"));
        rules.add(new Rule("SIDE_PANEL", "사이드패널|SIDE PANEL|로커|스텝|실패널"));
        rules.add(new Rule("PILLAR", "필러|필라|PILLAR|COLUMN|센터필러|A필러|B필러|C필러|A 필라|B 필라|C 필라|A-COLUMN|B-COLUMN|C-COLUMN"));
        rules.add(new Rule("LAMP", "램프|전조등|헤드램프|테일램프|안개등|LAMP|LIGHT"));
        rules.add(new Rule("RADIATOR_SUPPORT", "라디에이터|라지에이터|RADIATOR|서포트|SUPPORT"));
        rules.add(new Rule("PANEL_FRAME", "패널|판넬|PANEL|프레임|FRAME|멤버|MEMBER|크로스"));
        rules.add(new Rule("SUSPENSION_STEERING", "타이 로드|TIE ROD|암|A-ARM|로워암|어퍼암|너클|스테빌|링크|쇼바|댐퍼|서스펜션|SUSPENSION|KNUCKLE|ARM|DAMPER|SHOCK|STABILIZER"));
        rules.add(new Rule("WHEEL_TIRE", "휠|타이어|WHEEL|TIRE"));
        rules.add(new Rule("GLASS", "유리|글라스|윈드쉴드|GLASS|WINDSHIELD"));
        rules.add(new Rule("AIRBAG", "에어백|AIRBAG|A/B"));
        rules.add(new Rule("SEAT_BELT", "벨트|시트벨트|버클|리트랙터|SEAT BELT|LAP PT|PRETENSIONER|BUCKLE|RETRACTOR"));
        rules.add(new Rule("SEAT", "시트|SEAT|헤드레스트|HEADREST"));
        rules.add(new Rule("EMBLEM_STICKER", "엠블렘|엠블램|스티커|심볼|마크|로고|EMBLEM|STICKER|SYMBOL|LOGO|SIGN"));
        rules.add(new Rule("COATING_SEALER", "코팅|실런트|실러|UNDERCOATING|SEALER"));
        rules.add(new Rule("MIRROR", "미러|백미러|사이드미러|MIRROR"));
        rules.add(new Rule("GRILLE", "그릴|라디에이터그릴|GRILLE"));
        rules.add(new Rule("MOULDING_TRIM", "몰딩|가니쉬|트림|웨더스트립|웨더 스트립|MOULDING|MOLDING|GARNISH|TRIM|WEATHERSTRIP"));
        rules.add(new Rule("COVER_PROTECTOR", "커버|카바|프로텍터|가드|커튼|머드 플랩|MUDFLAP|MUD FLAP|COVER|PROTECTOR|GUARD|CURTAIN"));
        rules.add(new Rule("WIPER", "와이퍼|WIPER"));
        rules.add(new Rule("COOLING_AC", "콘덴서|컨덴서|쿨런트|쿨러|에어컨|A/C|CONDENSER|COOLANT|COOLER"));
        rules.add(new Rule("BRAKE", "브레이크|BRAKE"));
        rules.add(new Rule("EXHAUST", "머플러|마후라|배기|촉매|소음기|MUFFLER|EXHAUST|CATALYST"));
        rules.add(new Rule("BATTERY", "배터리|BATTERY"));
        rules.add(new Rule("VALVE_HOSE", "밸브|호스|파이프|튜브|냉매|VALVE|HOSE|PIPE|TUBE|BUNDLE"));
        rules.add(new Rule("BRACKET_MOUNT", "브라켓|브래킷|마운트|지지대|BRACKET|MOUNT"));
        rules.add(new Rule("POWERTRAIN", "엔진|미션|변속기|기어박스|클러치|ENGINE|MISSION|TRANSMISSION|GEARBOX|CLUTCH"));
        rules.add(new Rule("ELECTRICAL", "배선|와이어|하네스|스위치|센서|카메라|WIRING|HARNESS|SWITCH|SENSOR|CAMERA|ELECTRICAL"));
        rules.add(new Rule("FASTENER_SEAL", "가스켓|볼트|너트|클립|리벳|GASKET|BOLT|NUT|CLIP|RIVET"));
        rules.add(new Rule("TOWING", "견인|토잉|TOWING"));
        rules.add(new Rule("RAIL", "레일|RAIL"));
        return Collections.unmodifiableList(rules);
    }

    private static List<String> structuralSignals() {
        List<String> signals = new ArrayList<String>();
        Collections.addAll(signals,
                "필러", "PILLAR", "프레임", "FRAME", "멤버", "MEMBER",
                "라디에이터", "라지에이터", "RADIATOR", "서포트", "SUPPORT",
                "휠하우스", "WHEEL HOUSE", "사이드멤버", "크로스멤버",
                "플로어", "FLOOR", "바닥", "인사이드패널", "내측", "패널어셈블리");
        return Collections.unmodifiableList(signals);
    }

    private static List<String> highSeveritySignals() {
        List<String> signals = new ArrayList<String>();
        Collections.addAll(signals,
                "에어백", "AIRBAG", "A/B", "시트벨트", "PRETENSIONER",
                "라디에이터", "라지에이터", "RADIATOR", "프레임", "FRAME",
                "멤버", "MEMBER", "필러", "PILLAR", "휠하우스");
        return Collections.unmodifiableList(signals);
    }

    private static final class Rule {
        private final String code;
        private final Pattern pattern;

        private Rule(String code, String regex) {
            this.code = code;
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }
}
