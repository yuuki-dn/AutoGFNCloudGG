package dev.hachikuu.autogfncloudgg.utils;

import java.util.Random;

public class RandomString {
    private static final String number = "0123456789";
    private static final String lowercase = "abcdefghijklmnopqrstuvwxyz";
    private static final String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final byte RANDOM = 0;
    public static final byte NUMBER = 1;
    public static final byte LETTER = 2;
    public static final byte UPPER = 3;
    public static final byte LOWER = 4;
    public static final byte SPECIAL = 5;

    public boolean allow_number = false;
    public boolean allow_lowercase = true;
    public boolean allow_uppercase = false;
    public String special_characters = "";
    public int begin = 0;

    private final Random random = new Random();

    public final RandomString allowNumber(boolean state) {
        this.allow_number = state;
        return this;
    }

    public final RandomString allowLowerCase(boolean state) {
        this.allow_lowercase = state;
        return this;
    }

    public final RandomString allowUpperCase(boolean state) {
        this.allow_uppercase = state;
        return this;
    }

    public final RandomString beginBy(byte value) {
        this.begin = value;
        return this;
    }

    public final RandomString addSpecialCharacters(String chars) {
        for (char s : chars.toCharArray()) {
            String to_str = "" + s;
            if (!this.special_characters.contains(to_str)) this.special_characters.concat(to_str);
        }
        return this;
    }

    public final String generate(int size) {
        if (size < 1) return "";

        String random_char = new String(special_characters);
        if (allow_number) random_char += number;
        if (allow_lowercase) random_char += lowercase;
        if (allow_uppercase) random_char += uppercase;
        if (random_char.length() == 0) return "";

        String s = "";
        switch (begin) {
            case NUMBER:
                s += number.charAt(random.nextInt(number.length()));
                break;
            case LETTER:
                s += (lowercase + uppercase).charAt(random.nextInt(lowercase.length() + uppercase.length()));
                break;
            case UPPER:
                s += uppercase.charAt(random.nextInt(uppercase.length()));
                break;
            case LOWER:
                s += lowercase.charAt(random.nextInt(lowercase.length()));
                break;
            case SPECIAL:
                if (special_characters.length() == 0) break;
                s += special_characters.charAt(random.nextInt(special_characters.length()));
                break;

            default: break;
        }
        while (s.length() < size) s += random_char.charAt(random.nextInt(random_char.length()));
        return s;
    }
}
