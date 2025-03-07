package art.snail.naillian.backend.domain.nail.entity;

public enum NailCategory {
    ONE_COLOR(0),
    FRENCH(1),
    GRADIENT(2),
    ART(3);

    private final int index;

    NailCategory(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}