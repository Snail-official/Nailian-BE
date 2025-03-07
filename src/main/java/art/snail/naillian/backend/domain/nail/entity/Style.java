package art.snail.naillian.backend.domain.nail.entity;

public enum Style {
    WEDDING(0, "WEDDING"),
    KITSCH(1, "KITSCH"),
    CLEAN(2, "CLEAN");

    private final int id;
    private final String name;

    Style(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
