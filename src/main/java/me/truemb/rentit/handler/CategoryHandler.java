package me.truemb.rentit.handler;

public class CategoryHandler {
	
	private int catID = -1;
	private String alias = "";
	
	private double price = 0D;
	private String time = "";
	private int size = -1;
	private int maxSite = 1;
	
	public CategoryHandler(int catID, double price, String time) {
		this.catID = catID;
		this.price = price;
		this.time = time;
	}

	
	//GET METHODES
	public String getTime() {
		return this.time;
	}

	public int getSize() {
		return this.size;
	}

	public double getPrice() {
		return this.price;
	}

	public int getCatID() {
		return this.catID;
	}
	
	public int getMaxSite() {
		return this.maxSite;
	}
	
	public String getAlias() {
		return this.alias;
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

	public void setMaxSite(int maxSite) {
		this.maxSite = maxSite;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
	}
}
