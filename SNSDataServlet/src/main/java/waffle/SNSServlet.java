package main.java.waffle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.JSONException;

@WebServlet("/sns_serv")
public class SNSServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private Connection con = null;

	{
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://66.212.27.13:3306/arikuist_joke_db?serverTimezone=JST", "arikuist_Yutaroh", "********");
			//con = DriverManager.getConnection("jdbc:mysql://localhost:3306/joke_db?serverTimezone=JST", "root", "********");
		} catch (SQLException e) {

			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {


	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO 自動生成されたメソッド・スタブ
		super.doGet(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (con == null) {
			return;
		}

		String reqToken = req.getParameter("token");
		UserData userData = obtainUserDataFromToken(reqToken);

		resp.setContentType("application/json;charset=UTF-8");
		PrintWriter out = resp.getWriter();

		int result = 0;
		int point = 0;
		try {
			Statement statement = con.createStatement();
			String sqlSentence = "insert into user (line_id, line_name, picture_url, user_registration_date) values ('"
					+ userData.getUserId() + "','" + userData.getDisplayName() + "','" + userData.getPictureUrl() + "',NOW());";
			result = statement.executeUpdate(sqlSentence);
			out.write("Registered your data");
			return;
		} catch (SQLIntegrityConstraintViolationException e) {
			try {
			Statement statement = con.createStatement();
			String sqlSentence = "select point from user where line_id='" + userData.getUserId() + "';";
			ResultSet resultSet = statement.executeQuery(sqlSentence);

			while (resultSet.next()) {
				point = resultSet.getInt(1);
			}
			out.write(Integer.toString(point));
			return;
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		} catch (SQLException e) {

			e.printStackTrace();
		}
		out.write("Failed to Register your data");
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (con == null) {
			return;
		}

		String reqToken = req.getParameter("token");
		String reqPoint = req.getParameter("point");
		UserData userData = obtainUserDataFromToken(reqToken);

		resp.setContentType("application/json;charset=UTF-8");
		PrintWriter out = resp.getWriter();

		int result = 0;
		try {

			Statement statement = con.createStatement();
			String sqlSentence = "update user set point=" + reqPoint
					+ " where line_id='" + userData.getUserId() + "';";

			result = statement.executeUpdate(sqlSentence);
			System.out.println("database update state code = " + result);
			out.write("Success in updating your point");
			return;
		} catch (SQLException e) {

			e.printStackTrace();
		}
		out.write("Failed to updating your point");
	}

	/**
	 * LineAPIのアクセストークンから、ユーザーデータを取得する
	 * @param token トークン
	 * @return ユーザーデータ
	 */
	private UserData obtainUserDataFromToken (String token) {
		HttpURLConnection urlCon = null;
		String urlStr = "https://api.line.me/v2/profile";
		StringBuilder builder = new StringBuilder();

		try {
			URL url = new URL(urlStr);
			urlCon = (HttpURLConnection)url.openConnection();
			urlCon.setRequestMethod("GET");
			urlCon.setRequestProperty("Authorization","Bearer " + token);
			urlCon.setDoOutput(true);
			PrintWriter pr = new PrintWriter(urlCon.getOutputStream());
			pr.close();

			BufferedReader br = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
			String line = "";
			while ((line = br.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(builder.toString());

		UserData userData = null;

		try {
			userData = JSON.decode(builder.toString(), UserData.class);
		} catch (JSONException e) {
			System.out.println(e.getErrorCode());
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return userData;
	}

}



