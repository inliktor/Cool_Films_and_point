package amaneko.ml_and_fx.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class Genre {
    private int id;
    private String name;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public Genre(int id, String name) {
        this.id = id;
        this.name = name;
        this.selected.set(false);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
    public BooleanProperty selectedProperty() { return selected; }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Genre genre = (Genre) obj;
        return id == genre.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
