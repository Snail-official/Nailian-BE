package art.snail.naillian.backend.domain.nail.entity;

public enum NailColor {
    WHITE(0),
    BLACK(1),
    BEIGE(2),
    PINK(3),
    YELLOW(4),
    GREEN(5),
    BLUE(6),
    SILVER(7);

    private final int index;

    NailColor(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}