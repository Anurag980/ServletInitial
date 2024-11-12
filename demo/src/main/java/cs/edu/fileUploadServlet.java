package cs.edu;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/fileUploadServlet")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 10, // 10 MB
        maxFileSize = 1024 * 1024 * 50, // 50 MB
        maxRequestSize = 1024 * 1024 * 100) // 100 MB
public class fileUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 205242440643911308L;

    private static final String UPLOAD_DIR = "uploaded_files";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/abc"; // Replace 'abc' with your actual database name
    private static final String USER = "root"; // Replace with your MySQL username
    private static final String PASSWORD = "Anurag@531"; // Replace with your MySQL password

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String applicationPath = request.getServletContext().getRealPath("");
        String uploadFilePath = applicationPath + File.separator + UPLOAD_DIR;
        File fileSaveDir = new File(uploadFilePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdirs();
        }

        System.out.println("Upload File Directory=" + fileSaveDir.getAbsolutePath());
        String fileName = "";

        // Save uploaded file
        for (Part part : request.getParts()) {
            fileName = getFileName(part);
            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
            part.write(uploadFilePath + File.separator + fileName);
        }

        // Read content from uploaded file
        String message = "Result";
        String content;
        try (Scanner scanner = new Scanner(new File(uploadFilePath + File.separator + fileName))) {
            content = scanner.useDelimiter("\\Z").next();
        } catch (IOException e) {
            content = "Error reading file content: " + e.getMessage();
            e.printStackTrace();
        }
        
        response.getWriter().write(content);

        // Insert file details into the database
        insertFileRecord(fileName, content, response);
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        System.out.println("content-disposition header= " + contentDisp);
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "";
    }

    private void insertFileRecord(String fileName, String fileContent, HttpServletResponse response) throws IOException{
        String insertSQL = "INSERT INTO uploaded_files (file_name, file_content) VALUES (?, ?)";
    
        try {
            // Load the MySQL JDBC driver explicitly
            Class.forName("com.mysql.cj.jdbc.Driver");
    
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
    
                if (conn != null) {
                    System.out.println("Successfully connected to the database.");
                } else {
                    System.out.println("Failed to connect to the database.");
                    response.getWriter().println("Database connection failed.");
                    return;
                }
    
                pstmt.setString(1, fileName);
                pstmt.setString(2, fileContent);
    
                int rowsInserted = pstmt.executeUpdate();
                System.out.println("Inserted " + rowsInserted + " row(s) into the database.");
                response.getWriter().println("File details successfully inserted into the database.");
    
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                response.getWriter().println("Database error: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("MySQL JDBC Driver not found.");
        }
    }
}
