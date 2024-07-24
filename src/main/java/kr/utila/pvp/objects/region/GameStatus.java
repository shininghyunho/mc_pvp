package kr.utila.pvp.objects.region;

/**
 *  게임 상태
 *  게임 안에  경기로 이루어짐.
 *  게임 시작 -> 경기 시작/종료 -> 경기 시작/종료 -> 게임 종료
 */
public enum GameStatus {
    GAME_NOT_STARTED("게임_안함"),
    GAME_REQUESTED("게임_신청"),
    MATCH_INITIALIZED("경기_초기화"),
    MATCH_WAITING("경기_대기"),
    MATCH_IN_PROGRESS("경기_중"),
    MATCH_REPLAY_REQUESTED("경기_재시작_신청"),
    GAME_ENDED("게임_종료");

    private final String name;

    GameStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // is in game
    public boolean isInGame() {
        return this == MATCH_INITIALIZED || this == MATCH_WAITING || this == MATCH_IN_PROGRESS || this == MATCH_REPLAY_REQUESTED;
    }
}
