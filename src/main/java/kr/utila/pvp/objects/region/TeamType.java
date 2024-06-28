package kr.utila.pvp.objects.region;

public enum TeamType {

    BLUE, RED;

    public static TeamType getTeam(String name) {
        switch (name) {
            case "블루" -> {
                return BLUE;
            }
            case "레드" -> {
                return RED;
            }
        }
        return null;
    }

}
