package me.truemb.rentit.handler;

public class CategoryHandler {
	
	private int catID = -1;
	
	private double price = 0D;
	private String time = "";
	private int size = -1;
	
	public CategoryHandler(int catID, double price, String time) {
		this.catID = catID;
		this.price = price;
		this.time = time;
	}

	
	//GET METHODES
	public String getTime() {
		return time;
	}

	public int getSize() {
		return size;
	}

	public double getPrice() {
		return price;
	}

	public int getCatID() {
		return this.catID;
	}


	//SET METHODES
	public void setPrice(double price) {
		this.price = price;
	}
	
	public void setTime(String time) {
		this.time = time;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
