public class PinyinUtil {
    public static String getInitials(String chinese) {
        StringBuilder sb = new StringBuilder();
        for (char c : chinese.toCharArray()) {
            String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(c);
            if (pinyins != null && pinyins.length > 0) {
                sb.append(pinyins[0].charAt(0));
            } else {
                sb.append(c);
            }
        }
        return sb.toString().toLowerCase();
    }
}