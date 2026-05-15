import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;


public class TestViolations {


    public void veryLongMethod() {
        int a = 1;
        int b = 2;
        int c = 3;
        int d = 4;
        int e = 5;
        int f = 6;
        int g = 7;
        int h = 8;
        int i = 9;
        int j = 10;
        int k = 11;
        int l = 12;
        int m = 13;
        int n = 14;
        int o = 15;
        int p = 16;
        int q = 17;
        int r = 18;
        int s = 19;
        int t = 20;
        int u = 21;
        int v = 22;
        int w = 23;
        int x = 24;
        int y = 25;
        int z = 26;
        int aa = 27;
        int ab = 28;
        int ac = 29;
        int ad = 30;
        int ae = 31;
        int af = 32;
        System.out.println(a + b + c + d + e + f + g + h + i + j + k + l + m
                + n + o + p + q + r + s + t + u + v + w + x + y + z
                + aa + ab + ac + ad + ae + af);
    }

    public String highComplexityMethod(int x, int y, String s, boolean b) {
        String result = "";
        if (x > 0) {
            result += "positive";
        } else if (x < 0) {
            result += "negative";
        } else {
            result += "zero";
        }
        if (y > 10) {
            result += "-large";
        } else if (y > 5) {
            result += "-medium";
        } else if (y > 0) {
            result += "-small";
        } else {
            result += "-nonpositive";
        }
        if (s != null && !s.isEmpty()) {
            if (s.startsWith("A")) {
                result += "-A";
            } else if (s.startsWith("B")) {
                result += "-B";
            } else {
                result += "-other";
            }
        }
        if (b) {
            switch (result) {
                case "positive-large-A":
                    return "PLA";
                case "negative-small-B":
                    return "NSB";
                default:
                    return result + "-default";
            }
        }
        return result;
    }

    public void tooManyParams(int a, int b, int c, int d,
                               int e, int f, int g, int h) {
        System.out.println(a + b + c + d + e + f + g + h);
    }


    public void fanOutMethod() {
        List<String> list = new ArrayList<>();
        Map<String, Integer> map = new HashMap<>();
        Set<String> set = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        list.add("item");
        map.put("key", 1);
        set.add("elem");
        sb.append("hello");
    }

    public static void main(String[] args) {
        TestViolations tv = new TestViolations();
        tv.veryLongMethod();
        tv.highComplexityMethod(1, 7, "B", true);
        tv.tooManyParams(1, 2, 3, 4, 5, 6, 7, 8);
        tv.fanOutMethod();
    }
}
