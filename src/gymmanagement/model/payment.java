package gymmanagement.model;

public class payment {
	
	    private int id;
	    private int memberId;
	    private int amount;
	    private String date;

	    public payment() {}

	    public payment(int id, int memberId, int amount, String date) {
	        this.id = id;
	        this.memberId = memberId;
	        this.amount = amount;
	        this.date = date;
	    }

	    // Getters and setters (generate via IDE)
	    // ...
	}

