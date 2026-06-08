package android.graphics;

public class Color {
    public static int parseColor(String colorString) throws IllegalArgumentException {
        if (colorString.startsWith("#")) {
            long color = Long.parseLong(colorString.substring(1), 16);
            return (int) color;
        }
        throw new IllegalArgumentException("Unknown color");
    }
}
