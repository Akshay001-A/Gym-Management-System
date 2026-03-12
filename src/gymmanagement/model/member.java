package gymmanagement.model;

public class member {
    private int id;
    private String name, gymTime, email, aadhaar, gender, fatherName, mobile;
    private int age;
    private int amount;

    // Constructors
    public member() {}

    public member(int id, String name, int age, String mobile, String gymTime, String email,
                  String aadhaar, String gender, String fatherName, int amount) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.mobile = mobile;
        this.gymTime = gymTime;
        this.email = email;
        this.aadhaar = aadhaar;
        this.gender = gender;
        this.fatherName = fatherName;
        this.amount = amount;
    }

    // Getters and setters (generate via IDE)
    // ...
}