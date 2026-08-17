import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class RepairDictionaryBuilder {

    private static final String TABLE_NAME = "repair_mapping";
    private static final String ACTION_NAME_PATTERN =
            "1/2\\s*OH|1/3\\s*OH|1/4\\s*OH|오버홀|수리|조정|도장|탈착|교환|판금";
    private static final Pattern TRAILING_PAREN_ACTION =
            Pattern.compile("\\s*\\((" + ACTION_NAME_PATTERN + ")\\)\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern TRAILING_ACTION =
            Pattern.compile("\\s*(" + ACTION_NAME_PATTERN + ")\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static void main(String[] args) throws Exception {
        Path source = args.length > 0 ? Paths.get(args[0]) : Paths.get("수리내역_전체.TXT");
        Path outputDir = args.length > 1 ? Paths.get(args[1]) : Paths.get("data");

        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Source file not found: " + source.toAbsolutePath());
        }

        Files.createDirectories(outputDir);

        List<String> lines = readSourceLines(source);
        Set<String> seenKeys = new LinkedHashSet<String>();
        List<Item> items = new ArrayList<Item>();

        for (String line : lines) {
            if (line == null || line.trim().length() == 0) {
                continue;
            }

            String rawName = cleanText(line);
            String normalizedKey = normalizeKey(rawName);
            if (normalizedKey.length() == 0 || !seenKeys.add(normalizedKey)) {
                continue;
            }

            String text = rawName.toUpperCase(Locale.ROOT);

            Item item = new Item();
            item.mappingId = items.size() + 1;
            item.rawName = rawName;
            item.normalizedKey = normalizedKey;
            item.standardName = rawName;
            item.sideCode = match(text, sideRules(), "UNKNOWN");
            item.positionCode = match(text, positionRules(), "UNKNOWN");
            item.actionCode = inferAction(rawName, text);
            item.categoryCode = match(text, categoryRules(), "UNKNOWN");
            item.structuralFlag = containsAny(text, structuralSignals()) ? "Y" : "N";
            item.severityHint = inferSeverity(text, item.actionCode);
            item.activeFlag = "Y";
            items.add(item);
        }

        writeCsv(outputDir.resolve("repair_mapping_dictionary.csv"), items);
        writeJson(outputDir.resolve("repair_mapping_dictionary.json"), items);
        writeSql(outputDir.resolve("repair_mapping_seed.sql"), items);
        writeSummary(outputDir.resolve("repair_mapping_summary.md"), source, lines.size(), items.size());

        System.out.println("source_lines=" + lines.size());
        System.out.println("dictionary_items=" + items.size());
        System.out.println("csv=" + outputDir.resolve("repair_mapping_dictionary.csv"));
        System.out.println("json=" + outputDir.resolve("repair_mapping_dictionary.json"));
        System.out.println("sql=" + outputDir.resolve("repair_mapping_seed.sql"));
        System.out.println("summary=" + outputDir.resolve("repair_mapping_summary.md"));
    }

    private static List<String> readSourceLines(Path source) throws IOException {
        if (source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md")) {
            return Files.readAllLines(source, StandardCharsets.UTF_8);
        }
        try {
            return Files.readAllLines(source, Charset.forName("MS949"));
        } catch (MalformedInputException ex) {
            return Files.readAllLines(source, StandardCharsets.UTF_8);
        }
    }

    private static String cleanText(String value) {
        String cleaned = value.trim();
        cleaned = cleaned.replaceAll("\\(\\s*\\)$", "");
        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned;
    }

    private static String normalizeKey(String value) {
        String normalized = cleanText(value).toUpperCase(Locale.ROOT);
        normalized = normalized.replaceAll("[\"']", "");
        normalized = normalized.replaceAll("\\s+", "");
        normalized = normalized.replaceAll("[\\|\\-_·ㆍ]", "");
        normalized = normalized.replace('（', '(').replace('）', ')');
        return normalized;
    }

    private static String match(String text, List<Rule> rules, String defaultValue) {
        for (Rule rule : rules) {
            if (rule.pattern.matcher(text).find()) {
                return rule.code;
            }
        }
        return defaultValue;
    }

    private static String inferSeverity(String text, String actionCode) {
        if (containsAny(text, highSeveritySignals())) {
            return "HIGH";
        }
        if ("REPLACE".equals(actionCode) || "PANEL_BEATING".equals(actionCode)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static boolean containsAny(String text, List<String> signals) {
        for (String signal : signals) {
            if (text.contains(signal.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String inferAction(String rawName, String text) {
        String explicitAction = actionNameToCode(extractTrailingActionName(rawName));
        if (!"NOT_SPECIFIED".equals(explicitAction)) {
            return explicitAction;
        }
        return match(text, actionRules(), "NOT_SPECIFIED");
    }

    private static String extractTrailingActionName(String value) {
        String cleaned = cleanText(value);
        java.util.regex.Matcher parenMatcher = TRAILING_PAREN_ACTION.matcher(cleaned);
        if (parenMatcher.find()) {
            return parenMatcher.group(1);
        }

        java.util.regex.Matcher plainMatcher = TRAILING_ACTION.matcher(cleaned);
        if (plainMatcher.find()) {
            return plainMatcher.group(1);
        }

        return "";
    }

    private static String actionNameToCode(String actionName) {
        if (actionName == null) {
            return "NOT_SPECIFIED";
        }
        String normalized = actionName.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if ("1/2OH".equals(normalized) || "1/3OH".equals(normalized)
                || "1/4OH".equals(normalized) || "오버홀".equals(normalized)) {
            return "OVERHAUL";
        }
        if ("수리".equals(normalized) || "조정".equals(normalized)) {
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
        return rules;
    }

    private static List<Rule> positionRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule("FRONT", "FRONT|FRT|프론트|프런트|앞|전방|전면|전휀더|본네트|후드|라디에이터|라지에이터|헤드램프|전조등"));
        rules.add(new Rule("REAR", "REAR|RR|BACK|리어|뒤|후방|후면|트렁크|테일|백도어|리야"));
        rules.add(new Rule("SIDE", "SIDE|사이드|도어|쿼터|휀더|펜더|필러|실|스텝|로커"));
        rules.add(new Rule("UNDER", "UNDER|언더|하부|플로어|바닥|머플러|서스펜션|로워|LOWER"));
        rules.add(new Rule("UPPER", "UPPER|상부|루프|천정|ROOF"));
        return rules;
    }

    private static List<Rule> actionRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule("OVERHAUL", "1/2\\s*OH|1/3\\s*OH|1/4\\s*OH|오버홀|OVERHAUL"));
        rules.add(new Rule("REPLACE", "교환|교체|대체|REPLACE|ASSY|어셈블리|컴플리트|COMPLETE"));
        rules.add(new Rule("PAINT", "도장|페인트|PAINT|투톤|스프레이|코팅"));
        rules.add(new Rule("PANEL_BEATING", "판금|BEATING|수정|보수"));
        rules.add(new Rule("REMOVE_INSTALL", "탈착|탈부착|분해|장착|REMOVE|INSTALL|탈거"));
        rules.add(new Rule("ADJUST", "조정|교정|얼라인|ALIGN|수리|점검"));
        return rules;
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
        return rules;
    }

    private static List<String> structuralSignals() {
        List<String> signals = new ArrayList<String>();
        Collections.addAll(signals,
                "필러", "PILLAR", "프레임", "FRAME", "멤버", "MEMBER",
                "라디에이터", "라지에이터", "RADIATOR", "서포트", "SUPPORT",
                "휠하우스", "WHEEL HOUSE", "사이드멤버", "크로스멤버",
                "플로어", "FLOOR", "바닥", "인사이드패널", "내측", "패널어셈블리");
        return signals;
    }

    private static List<String> highSeveritySignals() {
        List<String> signals = new ArrayList<String>();
        Collections.addAll(signals,
                "에어백", "AIRBAG", "A/B", "시트벨트", "PRETENSIONER",
                "라디에이터", "라지에이터", "RADIATOR", "프레임", "FRAME",
                "멤버", "MEMBER", "필러", "PILLAR", "휠하우스");
        return signals;
    }

    private static void writeCsv(Path path, List<Item> items) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        try {
            writer.write("mapping_id,raw_name,normalized_key,standard_name,side_code,position_code,action_code,category_code,structural_flag,severity_hint,active_flag");
            writer.newLine();
            for (Item item : items) {
                writer.write(csv(item.mappingId));
                writer.write(",");
                writer.write(csv(item.rawName));
                writer.write(",");
                writer.write(csv(item.normalizedKey));
                writer.write(",");
                writer.write(csv(item.standardName));
                writer.write(",");
                writer.write(csv(item.sideCode));
                writer.write(",");
                writer.write(csv(item.positionCode));
                writer.write(",");
                writer.write(csv(item.actionCode));
                writer.write(",");
                writer.write(csv(item.categoryCode));
                writer.write(",");
                writer.write(csv(item.structuralFlag));
                writer.write(",");
                writer.write(csv(item.severityHint));
                writer.write(",");
                writer.write(csv(item.activeFlag));
                writer.newLine();
            }
        } finally {
            writer.close();
        }
    }

    private static void writeJson(Path path, List<Item> items) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        try {
            writer.write("[");
            writer.newLine();
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                writer.write("  {");
                writer.write(jsonField("mapping_id", String.valueOf(item.mappingId), false));
                writer.write(", ");
                writer.write(jsonField("raw_name", item.rawName, true));
                writer.write(", ");
                writer.write(jsonField("normalized_key", item.normalizedKey, true));
                writer.write(", ");
                writer.write(jsonField("standard_name", item.standardName, true));
                writer.write(", ");
                writer.write(jsonField("side_code", item.sideCode, true));
                writer.write(", ");
                writer.write(jsonField("position_code", item.positionCode, true));
                writer.write(", ");
                writer.write(jsonField("action_code", item.actionCode, true));
                writer.write(", ");
                writer.write(jsonField("category_code", item.categoryCode, true));
                writer.write(", ");
                writer.write(jsonField("structural_flag", item.structuralFlag, true));
                writer.write(", ");
                writer.write(jsonField("severity_hint", item.severityHint, true));
                writer.write(", ");
                writer.write(jsonField("active_flag", item.activeFlag, true));
                writer.write("}");
                if (i < items.size() - 1) {
                    writer.write(",");
                }
                writer.newLine();
            }
            writer.write("]");
            writer.newLine();
        } finally {
            writer.close();
        }
    }

    private static void writeSql(Path path, List<Item> items) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        try {
            writer.write("CREATE TABLE " + TABLE_NAME + " (");
            writer.newLine();
            writer.write("    mapping_id BIGINT NOT NULL PRIMARY KEY,");
            writer.newLine();
            writer.write("    raw_name VARCHAR(500) NOT NULL,");
            writer.newLine();
            writer.write("    normalized_key VARCHAR(500) NOT NULL,");
            writer.newLine();
            writer.write("    standard_name VARCHAR(500) NOT NULL,");
            writer.newLine();
            writer.write("    side_code VARCHAR(20) NOT NULL,");
            writer.newLine();
            writer.write("    position_code VARCHAR(20) NOT NULL,");
            writer.newLine();
            writer.write("    action_code VARCHAR(30) NOT NULL,");
            writer.newLine();
            writer.write("    category_code VARCHAR(40) NOT NULL,");
            writer.newLine();
            writer.write("    structural_flag CHAR(1) NOT NULL,");
            writer.newLine();
            writer.write("    severity_hint VARCHAR(20) NOT NULL,");
            writer.newLine();
            writer.write("    active_flag CHAR(1) NOT NULL,");
            writer.newLine();
            writer.write("    created_at TIMESTAMP NULL");
            writer.newLine();
            writer.write(");");
            writer.newLine();
            writer.newLine();
            writer.write("CREATE UNIQUE INDEX ux_repair_mapping_normalized_key ON " + TABLE_NAME + " (normalized_key);");
            writer.newLine();
            writer.write("CREATE INDEX ix_repair_mapping_codes ON " + TABLE_NAME + " (category_code, action_code, position_code, side_code);");
            writer.newLine();
            writer.newLine();

            for (Item item : items) {
                writer.write("INSERT INTO " + TABLE_NAME + " (mapping_id, raw_name, normalized_key, standard_name, side_code, position_code, action_code, category_code, structural_flag, severity_hint, active_flag, created_at) VALUES (");
                writer.write(String.valueOf(item.mappingId));
                writer.write(", '");
                writer.write(sql(item.rawName));
                writer.write("', '");
                writer.write(sql(item.normalizedKey));
                writer.write("', '");
                writer.write(sql(item.standardName));
                writer.write("', '");
                writer.write(sql(item.sideCode));
                writer.write("', '");
                writer.write(sql(item.positionCode));
                writer.write("', '");
                writer.write(sql(item.actionCode));
                writer.write("', '");
                writer.write(sql(item.categoryCode));
                writer.write("', '");
                writer.write(sql(item.structuralFlag));
                writer.write("', '");
                writer.write(sql(item.severityHint));
                writer.write("', '");
                writer.write(sql(item.activeFlag));
                writer.write("', CURRENT_TIMESTAMP);");
                writer.newLine();
            }
        } finally {
            writer.close();
        }
    }

    private static void writeSummary(Path path, Path source, int lineCount, int itemCount) throws IOException {
        List<String> lines = new ArrayList<String>();
        lines.add("# 수리내역 매핑 사전 생성 결과");
        lines.add("");
        lines.add("- 원본 파일: `" + source + "`");
        lines.add("- 원본 인코딩: `" + sourceEncodingDescription(source) + "`");
        lines.add("- 전체 라인 수: " + lineCount);
        lines.add("- 중복 제거 후 사전 항목 수: " + itemCount);
        lines.add("- CSV: `data/repair_mapping_dictionary.csv`");
        lines.add("- JSON: `data/repair_mapping_dictionary.json`");
        lines.add("- SQL: `data/repair_mapping_seed.sql`");
        lines.add("");
        lines.add("## 컬럼");
        lines.add("");
        lines.add("| 컬럼 | 설명 |");
        lines.add("| --- | --- |");
        lines.add("| mapping_id | 사전 항목 ID |");
        lines.add("| raw_name | 원본 수리내역 정제 문자열 |");
        lines.add("| normalized_key | 매칭용 정규화 키 |");
        lines.add("| standard_name | 표준 표시명 초기값 |");
        lines.add("| side_code | LEFT, RIGHT, BOTH, UNKNOWN |");
        lines.add("| position_code | FRONT, REAR, SIDE, UNDER, UPPER, UNKNOWN |");
        lines.add("| action_code | REPLACE, PAINT, PANEL_BEATING, REMOVE_INSTALL, ADJUST, NOT_SPECIFIED |");
        lines.add("| category_code | 부품/작업 카테고리 |");
        lines.add("| structural_flag | 골격/내부 영향 신호 여부 |");
        lines.add("| severity_hint | LOW, MEDIUM, HIGH |");
        lines.add("| active_flag | 사용 여부 |");
        lines.add("");
        lines.add("## 주의");
        lines.add("");
        lines.add("이 사전은 원본 텍스트에서 휴리스틱으로 생성한 초기 매핑이다. 운영 품질을 위해 미매핑/오분류 항목을 검수하면서 표준명, 카테고리, 사고 강도 신호를 보정해야 한다.");
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static String sourceEncodingDescription(Path source) {
        if (source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md")) {
            return "UTF-8";
        }
        return "MS949 우선, 실패 시 UTF-8 fallback";
    }

    private static String csv(Object value) {
        String text = String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static String jsonField(String name, String value, boolean quote) {
        if (quote) {
            return "\"" + json(name) + "\": \"" + json(value) + "\"";
        }
        return "\"" + json(name) + "\": " + value;
    }

    private static String json(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
            }
        }
        return builder.toString();
    }

    private static String sql(String value) {
        return value.replace("'", "''");
    }

    private static final class Rule {
        final String code;
        final Pattern pattern;

        Rule(String code, String regex) {
            this.code = code;
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    private static final class Item {
        int mappingId;
        String rawName;
        String normalizedKey;
        String standardName;
        String sideCode;
        String positionCode;
        String actionCode;
        String categoryCode;
        String structuralFlag;
        String severityHint;
        String activeFlag;
    }
}
