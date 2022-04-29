// Anna Boccuzzi HW4 Shop.java

import java.io.*;
import java.sql.*;
import java.util.Scanner;

class Shop {

	// the host name of the server and the server instance name/id
	public static final String oracleServer = "dbs3.cs.umb.edu";
	public static final String oracleServerSid = "dbs3";

	public static void main(String args[]) {
		Connection conn = null;
		conn = getConnection();
		if (conn == null)
			System.exit(1);

		Scanner input = new Scanner(System.in);
		try {
			String sql = "SELECT * FROM CUSTOMERS WHERE CID=?";
			PreparedStatement stmt = conn.prepareStatement(sql);

			while (true) {
				// get customer id from user
				System.out.print("Customer ID: ");
				int customer_id = input.nextInt();
				if (customer_id == -1) {
					System.out.println("Enter name: ");
					String customerName = input.next();
					System.out.println("Enter Customer Budget: ");
					double customerBudget = input.nextDouble();
					createCustomer(conn, customerName, customerBudget);
					
					//break;
				}
				stmt.setInt(1, customer_id);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					
					while (true) {
						System.out.println("Enter command: ");
						String command = input.next();
						switch (command) {
						case "P":
							getProducts(conn);
							break;
						case "O":
							System.out.println("Enter pid: ");
							int oPid = input.nextInt();
							System.out.println("Enter quantity: ");
							double oQuantity = input.nextDouble();
							processOrder(conn, oPid, oQuantity, customer_id);
							break;
						case "R":
							System.out.println("Product ID to Return: ");
							int returnPid = input.nextInt();
							processReturn(conn, returnPid, customer_id);
							break;
						case "S":
							System.out.println("Search for Product: ");
							String toSearch = input.next();
							productSearch(conn, toSearch);
							break;
						case "E":
							getExpenditure(conn, customer_id);
							break;
						case "C":
							getBudget(conn, customer_id);
							break;
						case "X":
							System.exit(0);
						}
					}
				} else
					if (customer_id != -1) {
						System.out.println("Incorrect Customer ID. ");
					}
			}
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
	}

	public static void createCustomer(Connection conn, String name, double budget) {
		int newCid = getMaxID(conn);
		newCid++;
		String insertNewCustomerSql = "INSERT INTO CUSTOMERS VALUES(?,?,?)";
		try {
			PreparedStatement insertNewCustomerStmt = conn.prepareStatement(insertNewCustomerSql);
			insertNewCustomerStmt.setInt(1, newCid);
			insertNewCustomerStmt.setString(2, name);
			insertNewCustomerStmt.setDouble(3, budget);
			ResultSet newCustomerRs = insertNewCustomerStmt.executeQuery();
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
		System.out.println("Customer created. ");
		System.out.println("ID Number is: " + newCid);
	}

	public static int getMaxID(Connection conn) {
		String getIdSql = "SELECT MAX(CID) AS MAXID FROM CUSTOMERS";
		int maxId = 0;
		try {
			PreparedStatement getIdStmt = conn.prepareStatement(getIdSql);
			ResultSet getIdRs = getIdStmt.executeQuery();
			if (getIdRs.next()) {
				do {
					maxId = getIdRs.getInt("MAXID");
				} while (getIdRs.next());
			} else
				System.out.println("No Products Found. ");
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
		return maxId;
	}

	public static void processOrder(Connection conn, int pid, double quantity, int cid) {
		boolean orderBool = pidCheck(conn, pid);
		boolean orderQuantBool = quantityCheck(conn, quantity);
		if (orderBool == true) {
			if (orderQuantBool == true) {
				double oPrice = getPrice(conn, pid);
				ResultSet ors = getSales(conn, cid, pid); // see what's in the table
				try {
					if (ors.next()) { // if its already in the sales table
						do {
							if (ors.getInt("quantity") > 0) { // if quantity > 0, no duplicate orders
								System.out.println("No duplicates allowed. ");
							} else if (ors.getInt("quantity") == 0) // it is in the table but the quantity is 0
								// update the quantity in sales
								updateSales(conn, cid, pid, quantity);
							// Subtract the total cost of this order from the budget of the active customer.
							subtractFromBudget(conn, oPrice, quantity, cid);
						} while (ors.next());
					} else // It isn't in the sales table,
						insertIntoSales(conn, cid, pid, quantity, oPrice);
				} catch (SQLException e) {
					System.out.println("ERROR OCCURRED");
					e.printStackTrace();
				}
			} else
				System.out.println("Invalid Quantity. ");
		} else
			System.out.println("Invalid Product Id. ");
	}

	public static boolean quantityCheck(Connection conn, double quantity) {
		boolean myQuantBool = false;
		if (quantity >= 1) {
			myQuantBool = true;
		} else {
			myQuantBool = false;
		}
		return myQuantBool;
	}

	public static void processReturn(Connection conn, int pid, int cid) {
		boolean itWasOrdered = wasOrdered(conn, pid, cid);
		double quantityOrdered = getQuantity(conn, cid, pid);
		if (itWasOrdered == true) {
			// if it was ordered, update the sales table to be quantity -1 and add cost back
			// to their budget
			quantityOrdered--;
			updateSales(conn, cid, pid, quantityOrdered);
			addToBudget(conn, pid, cid);
		}
	}

	public static void addToBudget(Connection conn, int pid, int cid) {
		String addBudgetSql = "UPDATE CUSTOMERS SET BUDGET = BUDGET+? WHERE CID=?";
		double productPrice = getPrice(conn, pid);
		try {
			PreparedStatement addBudgetStmt = conn.prepareStatement(addBudgetSql);
			addBudgetStmt.setDouble(1, productPrice);
			addBudgetStmt.setInt(2, cid);
			ResultSet addBudgetRs = addBudgetStmt.executeQuery();
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
	}

	public static double getQuantity(Connection conn, int cid, int pid) {
		String getQuantSql = "SELECT QUANTITY FROM SALES WHERE PID = ? AND CID = ?";
		double myQuant = 0;
		try {
			PreparedStatement quantStmt = conn.prepareStatement(getQuantSql);
			quantStmt.setInt(1, pid);
			quantStmt.setInt(2, cid);
			ResultSet quantRs = quantStmt.executeQuery();
			if (quantRs.next()) {
				do {
					myQuant = quantRs.getDouble("QUANTITY");
				} while (quantRs.next());
			} else
				System.out.println("No Products Found. ");
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
		return myQuant;
	}

	public static boolean wasOrdered(Connection conn, int pid, int cid) {
		// something was ordered if it is in the sales table and quantity is atleast 1
		String wasOrderedSql = "SELECT * FROM SALES WHERE PID = ? AND CID = ? AND QUANTITY >= 1";
		boolean ordered = false;
		try {
			PreparedStatement orderedStmt = conn.prepareStatement(wasOrderedSql);
			orderedStmt.setInt(1, pid);
			orderedStmt.setInt(2, cid);
			ResultSet wasOrderedRs = orderedStmt.executeQuery();
			if (wasOrderedRs.next()) {
				do {
					ordered = true;
				} while (wasOrderedRs.next());
			} else
				System.out.println("No Products Found. ");
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
		return ordered;
	}

	public static void productSearch(Connection conn, String prodName) {
		String prodSearchSql = "SELECT NAME, PRICE FROM PRODUCTS WHERE NAME LIKE ?";
		try {
			PreparedStatement prodSearchStmt = conn.prepareStatement(prodSearchSql);
			prodSearchStmt.setString(1, prodName + "%");
			ResultSet prodSearchRs = prodSearchStmt.executeQuery();
			if (prodSearchRs.next()) {
				do {
					String productName = prodSearchRs.getString("NAME");
					double productPrice = prodSearchRs.getDouble("PRICE");
					System.out.println("Product Name: " + productName + "\tPrice: " + productPrice);
				} while (prodSearchRs.next());
			} else
				System.out.println("No Products Found. ");
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
	}

	public static void getExpenditure(Connection conn, int cid) {
		// for case "E" : Lists all orders the current customer has made.
		// display the product ID, product name, and the total cost of the order
		// (quantity * price).
		String expenSql = "SELECT PID, QUANTITY FROM SALES WHERE CID = ?";
		int i = 0;
		try {
			PreparedStatement expenStmt = conn.prepareStatement(expenSql);
			expenStmt.setInt(1, cid);
			ResultSet expenRs = expenStmt.executeQuery();
			if (expenRs.next()) {
				do {
					i++;
					int productId = expenRs.getInt("PID");
					int productQuant = expenRs.getInt("QUANTITY");
					String productName = getProductName(conn, productId);
					double productPrice = getPrice(conn, productId);
					double totalCost = productQuant * productPrice;
					// Order 1 Pid: 1 Product Name: product1 Total:
					System.out.println("Order " + i);
					System.out.println("\tPid: " + productId);
					System.out.println("\tProduct Name: " + productName);
					System.out.println("\tTotal: " + totalCost);
				} while (expenRs.next());
			} else
				System.out.println("No Records Retrieved");
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}

	}

	public static String getProductName(Connection conn, int pid) {
		String pnameSql = "SELECT NAME FROM PRODUCTS WHERE PID = ?";
		String pName = "";
		try {
			PreparedStatement pnameStmt = conn.prepareStatement(pnameSql);
			pnameStmt.setInt(1, pid);
			ResultSet pnameRs = pnameStmt.executeQuery();
			if (pnameRs.next()) {
				do {
					pName = pnameRs.getString("NAME");
				} while (pnameRs.next());
			} else
				System.out.println("No Records Retrieved");
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
		return pName;
	}

	public static void getBudget(Connection conn, int cid) {
		// for case "c"
		String budgetSql = "SELECT BUDGET FROM CUSTOMERS WHERE CID =?";
		double currentBudget = 0;
		try {
			PreparedStatement budgetStmt = conn.prepareStatement(budgetSql);
			budgetStmt.setInt(1, cid);
			ResultSet budgetRs = budgetStmt.executeQuery();
			if (budgetRs.next()) {
				do {
					currentBudget = Double.parseDouble(budgetRs.getObject(1).toString());
					System.out.println("Budget: " + currentBudget);
				} while (budgetRs.next());
			} else
				System.out.println("No Records Retrieved");
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
	}

	public static ResultSet getSales(Connection conn, int cid, int pid) {
		// for case "o"
		String salesSql = "SELECT * FROM SALES WHERE CID = ? AND PID = ?";
		ResultSet salesSet = null;
		try {
			PreparedStatement salesStmt = conn.prepareStatement(salesSql);
			salesStmt.setInt(1, cid);
			salesStmt.setInt(2, pid);
			salesSet = salesStmt.executeQuery();
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
		return salesSet;
	}

	public static void updateSales(Connection conn, int cid, int pid, double quantity) {
		String oUpdateSql = "UPDATE SALES SET QUANTITY = ? WHERE CID = ? AND PID = ?";
		try {
			PreparedStatement oUpStmt = conn.prepareStatement(oUpdateSql);
			oUpStmt.setDouble(1, quantity);
			oUpStmt.setInt(2, cid);
			oUpStmt.setInt(3, pid);
			ResultSet oUpRs = oUpStmt.executeQuery();
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
	}

	public static void insertIntoSales(Connection conn, int cid, int pid, double quantity, double price) {
		// insert it into sales
		String insertSalesSql = "INSERT INTO SALES VALUES(?,?,?)";
		try {
			PreparedStatement insertSalesStmt = conn.prepareStatement(insertSalesSql);
			insertSalesStmt.setInt(1, pid);
			insertSalesStmt.setInt(2, cid);
			insertSalesStmt.setDouble(3, quantity);
			ResultSet insertSalesRs = insertSalesStmt.executeQuery();
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
		subtractFromBudget(conn, price, quantity, cid);
	}

	public static double getPrice(Connection conn, int pid) {
		// get the product price
		String priceSql = "SELECT PRICE FROM PRODUCTS WHERE PID=?";
		double myPrice = 0;
		try {
			PreparedStatement priceStmt = conn.prepareStatement(priceSql);
			priceStmt.setInt(1, pid);
			ResultSet priceRs = priceStmt.executeQuery();
			if (priceRs.next()) {
				do {
					myPrice = Double.parseDouble(priceRs.getObject(1).toString());
				} while (priceRs.next());
			} else
				System.out.println("No Records Retrieved");
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
		return myPrice;
	}

	public static void subtractFromBudget(Connection conn, double price, double quantity, int cid) {
		double total = (quantity * price);
		String budgetSql = "UPDATE CUSTOMERS SET BUDGET = BUDGET-? WHERE CID=?";
		try {
			PreparedStatement budgetStmt = conn.prepareStatement(budgetSql);
			budgetStmt.setDouble(1, total);
			budgetStmt.setInt(2, cid);
			ResultSet budgetRs = budgetStmt.executeQuery();
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
	}

	public static void getProducts(Connection conn) {
		// for case "p"
		String psql = "SELECT NAME, PID FROM PRODUCTS";
		try {
			PreparedStatement pStmt = conn.prepareStatement(psql);
			ResultSet prs = pStmt.executeQuery();
			if (prs.next()) {
				do {
					String pName = prs.getString("name");
					int pPid = prs.getInt("pid");
					double pPrice = getPrice(conn, pPid);
					System.out.println("Product: " + pName + "\tPrice: " + pPrice + "\t Pid: " + pPid);
				} while (prs.next());
			} else
				System.out.println("No Products Found. ");
		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
	}

	public static boolean pidCheck(Connection conn, int pid) {
		boolean myBool = false;
		String pidSql = "SELECT PID FROM PRODUCTS WHERE PID=?";
		try {
			PreparedStatement pidStmt = conn.prepareStatement(pidSql);
			pidStmt.setInt(1, pid);
			ResultSet pidRS = pidStmt.executeQuery();
			if (pidRS.next()) {
				myBool = true;
			} else
				myBool = false;

		} catch (SQLException e) {
			System.out.println("ERROR OCCURRED");
			e.printStackTrace();
		}
		return myBool;
	}

	public static Connection getConnection() {

		// first we need to load the driver
		String jdbcDriver = "oracle.jdbc.OracleDriver";
		try {
			Class.forName(jdbcDriver);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Get username and password
		Scanner input = new Scanner(System.in);
		System.out.print("Username:");
		String username = input.nextLine();
		System.out.print("Password:");
		// the following is used to mask the password
		Console console = System.console();
		String password = new String(console.readPassword());
		String connString = "jdbc:oracle:thin:@" + oracleServer + ":1521:" + oracleServerSid;

		System.out.println("Connecting to the database...");

		Connection conn;
		// Connect to the database
		try {
			conn = DriverManager.getConnection(connString, username, password);
			System.out.println("Connection Successful");
		} catch (SQLException e) {
			System.out.println("Connection ERROR");
			e.printStackTrace();
			return null;
		}

		return conn;
	}
}
