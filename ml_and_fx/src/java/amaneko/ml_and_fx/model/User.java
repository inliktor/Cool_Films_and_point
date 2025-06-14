package amaneko.ml_and_fx.model;

/**
 * Модель пользователя
 */
public class User {
    private int id;
    private String username;
    private int age;
    private String gender;
    
    // Конструкторы
    public User() {}
    
    public User(String username, int age, String gender) {
        this.username = username;
        this.age = age;
        this.gender = gender;
    }
    
    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                '}';
    }
}
