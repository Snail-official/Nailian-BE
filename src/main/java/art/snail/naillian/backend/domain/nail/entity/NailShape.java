package art.snail.naillian.backend.domain.nail.entity;

public enum NailShape {
    SQUARE(0),
    ROUND(1),
    ALMOND(2),
    BALLERINA(3),
    STILETTO(4);

    private final int index;

    NailShape(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}