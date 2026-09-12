**Forgot Password Flow**

Biodex  ·  Invasive Species Tracker  ·  Draft v1.0

## **Document Control**

| Field | Value |
| - | - |
| Module | Login/Signup Screen -\> Forgot Password -\> Reset Password |
| Page Owner | Bernard |
| Related 	Sub-pages | Login, Signup, Settings -\> Account (Change Password) |
| App | Biodex |
| Tech Stack | JavaFX (UI/Controllers), SQL via JDBC (persistence), JavaMail/SMTP 	(email) |
| Status | Draft v1.0 |


**Contents**

Document Control............................................................................... 1

1. Overview............................................................................................. 1

2. Scope Within the Login/Signup Screen.................................................. 1

3. Functional Requirements..................................................................... 1

4. UI Flow................................................................................................ 1

Step 1, Request Code........................................................................... 1

Step 2, Verify Code.............................................................................. 1

Step 3, Reset Password........................................................................ 1

5. Controller Architecture........................................................................ 1

6. Data Model and Schema...................................................................... 1

7. DAO Reference.................................................................................... 1

8. Validation and Error Handling............................................................... 1

9. Security Considerations........................................................................ 1

10. Testing Plan....................................................................................... 1

11. Suggested File Structure..................................................................... 1

# **1. Overview**

The Forgot Password flow allows users who cannot sign in to recover access to Biodex without contacting support. It is opened from the "Forgot Password?" hyperlink beneath the password field on the Login/Signup screen.

Because Biodex is a JavaFX desktop application, an emailed reset link cannot open directly in the app. The system therefore emails a 6-digit verification code for the user to enter in the application. Each code remains valid for 15 minutes.

# **2. Scope Within the Login/Signup Screen**

Login/Signup Screen  
|-- LoginView.fxml  
|     |-- username/password fields  
|     |-- Hyperlink "Forgot Password?" -\> opens ForgotPasswordView  (this spec)  
|     \`-- Hyperlink "Create an account" -\> opens SignupView  
|  
|-- SignupView.fxml (out of scope here)  
|  
\`-- Forgot Password wizard:  
ForgotPasswordView.fxml  (enter email, request code)  
-\> VerifyCodeView.fxml (enter 6-digit code)  
-\> ResetPasswordView.fxml (set new password)  
-\> back to LoginView with a "Password updated" banner

The Login screen starts a one-way wizard that finishes by returning the user to LoginView. Users cannot navigate directly to either of the later steps.

# **3. Functional Requirements**

| \# | Requirement | Priority |
| :-: | - | :-: |
| F0 | A "Forgot Password?" link appears below the password field on the Login screen | Must |
| F1 | A user starts password recovery by entering the email linked to their account | Must |
| F2 | The system sends a 6-digit email code that expires after 15 minutes | Must |
| F3 | The user submits the code, with invalid codes producing an inline error | Must |
| F4 | The user may request another code after a 60-second cooldown | Should |
| F5 | Each verification code can be used only once | Must |
| F6 | After verification, the user enters and confirms a new password | Must |
| F7 | The new password must meet the rules used by the Change Password feature | Must |
| F8 | A successful reset invalidates the user's other active sessions | Should |
| F9 | Step 1 behaves the same for registered and unregistered email addresses | Must |
| F10 | Expired or exhausted codes direct the user to "request a new code" | Must |
| F11 | Input fields are disabled and progress is shown while asynchronous work runs | Should |
| F12 | Password-reset requests are rate-limited by email address and IP address | Should |


# **4. UI Flow**

## **Step 1, Request Code**

This screen contains an email field, a "Send Code" button, and a "Back to Login" link. Any correctly formatted address receives the same confirmation and proceeds to Step 2, regardless of whether the address belongs to an account (F9).

## **Step 2, Verify Code**

The heading displays "We sent a code to \{maskedEmail\}" above the 6-digit code field and "Verify" button. The "Resend code" link remains disabled for 60 seconds after a code is sent. Five incorrect attempts invalidate the code and return the user to Step 1.

## **Step 3, Reset Password**

Provides new-password and confirmation fields, a live checklist of password rules, and a "Reset Password" button.

*\[Wireframe and basic mock-up included at the end of this document\]*

# **5. Controller Architecture**

All three controllers use a shared in-memory PasswordResetSession that stores the email, userId, and codeVerified flag. SessionManager continues to manage signed-in users and must remain separate from the reset session.

public class ForgotPasswordController implements Initializable \{

```
@FXML private TextField emailField;    
@FXML private Label errorLabel;    
  
private final UserDAO userDAO \\= new UserDAO(DatabaseConnection.getConnection());    
private final PasswordResetDAO resetDAO \\= new PasswordResetDAO(DatabaseConnection.getConnection());    
private final EmailService emailService \\= new EmailService();    
  
@FXML    
private void handleSendCode() \{    
    String email \\= emailField.getText().trim();    
    if (\\!Validator.isValidEmail(email)) \{    
        showError("Enter a valid email address.");    
        return;    
    \}    
    Task\\\<Void\\\> task \\= new Task\\\<\\\>() \{    
        protected Void call() \{    
            // Look up the user, but keep the UI path identical either way (F9)    
            User user \\= userDAO.findByEmail(email);    
            if (user \\!= null) \{    
                String code \\= CodeGenerator.sixDigit();    
                resetDAO.createResetCode(user.getUserId(), code, Duration.ofMinutes(15));    
                emailService.sendResetCode(email, code);    
            \}    
            return null;    
        \}    
    \};    
    task.setOnSucceeded(e \\-\\\> \{    
        PasswordResetSession.getInstance().setEmail(email);    
        SceneRouter.getInstance().goToVerifyCode();    
    \});    
    new Thread(task).start();    
\}    
  
private void showError(String msg) \{ errorLabel.setText(msg); errorLabel.setVisible(true); \}    
  
@Override public void initialize(URL location, ResourceBundle resources) \{\}  
```

\}

public class VerifyCodeController implements Initializable \{

```
@FXML private TextField codeField;    
@FXML private Label errorLabel;    
  
private final PasswordResetDAO resetDAO \\= new PasswordResetDAO(DatabaseConnection.getConnection());    
  
@FXML    
private void handleVerifyCode() \{    
    String code \\= codeField.getText().trim();    
    String email \\= PasswordResetSession.getInstance().getEmail();    
  
    Task\\\<Boolean\\\> task \\= new Task\\\<\\\>() \{    
        protected Boolean call() \{ return resetDAO.verifyCode(email, code); \}    
    \};    
    task.setOnSucceeded(e \\-\\\> \{    
        if (Boolean.TRUE.equals(task.getValue())) \{    
            PasswordResetSession.getInstance().setCodeVerified(true);    
            SceneRouter.getInstance().goToResetPassword();    
        \} else \{    
            errorLabel.setText("Incorrect or expired code.");    
            errorLabel.setVisible(true);    
        \}    
    \});    
    new Thread(task).start();    
\}    
  
@Override public void initialize(URL location, ResourceBundle resources) \{\}  
```

\}

public class ResetPasswordController implements Initializable \{

```
@FXML private PasswordField newPasswordField;    
@FXML private PasswordField confirmPasswordField;    
@FXML private Label errorLabel;    
  
private final UserDAO userDAO \\= new UserDAO(DatabaseConnection.getConnection());    
private final PasswordResetDAO resetDAO \\= new PasswordResetDAO(DatabaseConnection.getConnection());    
  
@FXML    
private void handleResetPassword() \{    
    if (\\!PasswordResetSession.getInstance().isCodeVerified()) \{    
        SceneRouter.getInstance().goToLogin();    
        return;    
    \}    
    String pw \\= newPasswordField.getText();    
    String confirm \\= confirmPasswordField.getText();    
  
    if (\\!Validator.isStrongPassword(pw)) \{ showError("Min 8 chars, 1 letter, 1 number."); return; \}    
    if (\\!pw.equals(confirm)) \{ showError("Passwords don't match."); return; \}    
  
    String email \\= PasswordResetSession.getInstance().getEmail();    
    Task\\\<Void\\\> task \\= new Task\\\<\\\>() \{    
        protected Void call() \{    
            User user \\= userDAO.findByEmail(email);    
            userDAO.updatePasswordHash(user.getUserId(), PasswordHasher.hash(pw));    
            resetDAO.invalidateAllCodesForUser(user.getUserId());    
            SessionManager.getInstance().invalidateOtherSessions(user.getUserId()); // F8    
            return null;    
        \}    
    \};    
    task.setOnSucceeded(e \\-\\\> \{    
        PasswordResetSession.getInstance().clear();    
        SceneRouter.getInstance().goToLoginWithBanner("Password updated. Please sign in.");    
    \});    
    new Thread(task).start();    
\}    
  
private void showError(String msg) \{ errorLabel.setText(msg); errorLabel.setVisible(true); \}    
  
@Override public void initialize(URL location, ResourceBundle resources) \{\}  
```

\}

# **6. Data Model and Schema**

public class PasswordResetCode \{  
private int resetId;  
private int userId;  
private String codeHash;   // hashed, not plaintext  
private Instant expiresAt;  
private int attemptCount;  
private boolean used;  
private Instant createdAt;  
private Instant lastSentAt; // resend cooldown  
\}

CREATE TABLE password\_reset\_codes (  
reset\_id   	INTEGER PRIMARY KEY AUTOINCREMENT,  
user\_id    	INTEGER NOT NULL,  
code\_hash  	VARCHAR(255) NOT NULL,  
expires\_at 	TIMESTAMP	NOT NULL,  
attempt\_count  INTEGER      DEFAULT 0,  
used       	BOOLEAN  	DEFAULT 0,  
created\_at 	TIMESTAMP	DEFAULT CURRENT\_TIMESTAMP,  
last\_sent\_at   TIMESTAMP    DEFAULT CURRENT\_TIMESTAMP,  
FOREIGN KEY (user\_id) REFERENCES users(user\_id) ON DELETE CASCADE  
);

CREATE INDEX idx\_reset\_user\_id ON password\_reset\_codes(user\_id);

No changes are required in the users table. Reset codes are stored separately, allowing expired and used records to remain without changing password\_hash until the new hash is saved in Step 3.

# **7. DAO Reference**

public class PasswordResetDAO \{

```
private final Connection connection;    
private static final int MAX\\\_ATTEMPTS \\= 5;    
  
public PasswordResetDAO(Connection connection) \{ this.connection \\= connection; \}    
  
public void createResetCode(int userId, String plainCode, Duration validFor) \{    
    invalidateAllCodesForUser(userId);    
    String sql \\= "INSERT INTO password\\\_reset\\\_codes (user\\\_id, code\\\_hash, expires\\\_at) VALUES (?, ?, ?)";    
    try (PreparedStatement ps \\= connection.prepareStatement(sql)) \{    
        ps.setInt(1, userId);    
        ps.setString(2, PasswordHasher.hash(plainCode));    
        ps.setTimestamp(3, Timestamp.from(Instant.now().plus(validFor)));    
        ps.executeUpdate();    
    \} catch (SQLException e) \{ throw new DataAccessException(e); \}    
\}    
  
public boolean verifyCode(String email, String plainCode) \{    
    String sql \\= "SELECT prc.\\\* FROM password\\\_reset\\\_codes prc "    
                \\+ "JOIN users u ON u.user\\\_id \\= prc.user\\\_id "    
                \\+ "WHERE u.email \\= ? AND prc.used \\= 0 "    
                \\+ "ORDER BY prc.created\\\_at DESC LIMIT 1";    
    try (PreparedStatement ps \\= connection.prepareStatement(sql)) \{    
        ps.setString(1, email);    
        ResultSet rs \\= ps.executeQuery();    
        if (\\!rs.next()) return false;    
        if (rs.getTimestamp("expires\\\_at").toInstant().isBefore(Instant.now())) return false;    
        if (rs.getInt("attempt\\\_count") \\\>= MAX\\\_ATTEMPTS) return false;    
  
        boolean matches \\= PasswordHasher.verify(plainCode, rs.getString("code\\\_hash"));    
        if (matches) markUsed(rs.getInt("reset\\\_id"));    
        else incrementAttempts(rs.getInt("reset\\\_id"));    
        return matches;    
    \} catch (SQLException e) \{ throw new DataAccessException(e); \}    
\}    
  
public void invalidateAllCodesForUser(int userId) \{    
    String sql \\= "UPDATE password\\\_reset\\\_codes SET used \\= 1 WHERE user\\\_id \\= ? AND used \\= 0";    
    try (PreparedStatement ps \\= connection.prepareStatement(sql)) \{    
        ps.setInt(1, userId);    
        ps.executeUpdate();    
    \} catch (SQLException e) \{ throw new DataAccessException(e); \}    
\}    
  
private void markUsed(int resetId) \{ /\\\* SET used \\= 1 WHERE reset\\\_id \\= ? \\\*/ \}    
private void incrementAttempts(int resetId) \{ /\\\* SET attempt\\\_count \\= attempt\\\_count \\+ 1 WHERE reset\\\_id \\= ? \\\*/ \}  
```

\}

All query values are bound using PreparedStatement, and no column names are constructed dynamically.

# **8. Validation and Error Handling**

| Field | Rule | Failure Behaviour |
| - | - | - |
| Email (Step 1) | Non-empty, 			valid format | Inline error; identical response whether or not an account exists |
| Code (Step 2) | Exactly 6 digits | Inline error; PasswordResetDAO records the failed attempt |
| Code expiry | PasswordResetDAO checks expires\_at | "Code expired. Request a new code." |
| Max attempts | The code is invalidated after 5 incorrect attempts | "Too many attempts. Request a new code." |
| New password | At least 8 characters, including 1 letter and 1 number | Inline error; submission remains disabled until the value is valid |
| Confirm	password | Must match the new password exactly | "Passwords do not match." |
| DB/email	failure | Any exception raised inside a Task | Non-blocking alert; the form remains available for another attempt |


# **9. Security Considerations**

•     Step 1 gives registered and unknown addresses the same response, preventing attackers from identifying which emails have accounts (F9).

•     PasswordResetDAO stores only a hash of each code by using the same PasswordHasher used for account passwords; no readable reset code is kept in the database.

•     Codes expire after 15 minutes, and verifyCode marks a matching code as used immediately.

•     verifyCode rejects a code after 5 incorrect guesses, limiting each issued code to 5 brute-force attempts.

•     ResetPasswordController invalidates the user's other active sessions after the new password hash is stored (F8).

•     SMTP credentials are stored in application configuration, and that configuration is excluded from Git.

•     Plaintext reset codes and new passwords must never be written to application logs.

# **10. Testing Plan**

**Unit tests.** Test PasswordResetDAO code creation, verification, expiry, and attempt limits, together with Validator password rules and code hashing.

**UI tests (TestFX).** Run the successful three-screen flow, followed by expired-code handling, lockout after 5 failures, password mismatch, and resend cooldown scenarios.

**Manual QA.** Compare requests for a real account and an unknown address to confirm identical behaviour. Complete an email round trip with a test inbox, then verify that a session already signed in on another machine is invalidated after the reset.

# **11. Suggested File Structure**

com.pesttracker  
|-- app/     	SceneRouter.java, SessionManager.java, PasswordResetSession.java  
|-- controller/  ForgotPasswordController.java, VerifyCodeController.java, ResetPasswordController.java  
|-- model/   	User.java, PasswordResetCode.java  
|-- dao/     	UserDAO.java, PasswordResetDAO.java  
|-- db/      	DatabaseConnection.java, schema.sql  
\`-- util/    	Validator.java, PasswordHasher.java, CodeGenerator.java, EmailService.java  
resources/  
|-- fxml/    	LoginView.fxml, ForgotPasswordView.fxml, VerifyCodeView.fxml, ResetPasswordView.fxml  
\`-- css/     	auth-theme.css

**Wireframe and basic mockup:**![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAlUAAAIbCAYAAADCVGuuAAAACXBIWXMAAAsTAAALEwEAmpwYAAAgAElEQVR4nOzde3hU5b33/8+EkAMk5AAmOJGAKBANEdREKYaKBIRKEQsUrVURdYvQq+KvTyt4qJUqCtXdLe4q0ro5SMUHEAzSoBxiRSIbmuADDakByinAQMIhCSEwgZD8/hhmkZADOdzJJJn367q4WJlZs9Z31qyZ+cy673UvW3R0dLkAAADQKD6eLgAAAKAtIFQBAAAYQKgCAAAwwDcuLs7TNQAAALR6viUlJZ6uAQAAoNWj+Q8AAMAAQhUAAIABhCoAAAADCFUAAAAGEKoAAAAMIFQBAAAYQKgCAAAwgFAFAABgAKEKAADAAEIVAACAAYQqAAAAAwhVAAAABhCqAAAADCBUAQAAGECoAgAAMIBQBQAAYAChCgAAwABCFQAAgAGEKgAAAAMIVQAAAAYQqgAAAAwgVAEAABhAqAIAADCAUAUAAGAAoQoAAMAAQhUAAIABhCoAAAADCFUAAAAGEKoAAAAMIFQBAAAYQKgCAAAwgFAFAABgAKEKAADAAEIVAACAAYQqAAAAAwhVAAAABhCqAAAADCBUAQAAGECoAgAAMIBQBQAAYAChCgAAwABCFQAAgAGEKgAAAAMIVQAAAAYQqgAAAAwgVAEAABhAqAIAADCAUAUAAGAAoQoAAMAAQhUAAIABhCoAAAADCFUAAAAGEKoAAAAMIFQBAAAYQKgCAAAwwPb5quRyTxcBAADQ2tnOFBUSqgAAABqJ5j8AAAADCFUAAAAGEKoAAAAMIFQBAAAYQKgCAAAwgFAFAABgAKEKAADAAEIVAACAAYQqAAAAAwhVAAAABhCqAAAADCBUAQAAGECoAgAAMIBQBQAAYAChCgAAwABCFQAAgAGEKgAAAAMIVQAAAAYQqgAAAAwgVAEAABhAqAIAADCAUAUAAGAAoaoF2rdvX633uf+5OZ1OZWVlVTvflbdlZGRUWWZGRob1eKfTWWkdTue5qy43Pz+/0t9Op1MZGRlVanSv2+l06ujRo9Zj3dPu/901VVzu0aNHq6zbfRvahpr2T7eK+4d0ed9x7xf5+fmV9l83p9NpPb7ivK77Lu/f1e1LGRkZVWqquC9XfKzr/eKsVG917xe0bu7P2ys/99zy8/OrvO4Vb3Pvx1fu79V9tle87cr11bTPXammfbW6uq+cdu/r1X0WV1xvdbV4q3YvvvjCq54uApft27dP8+Z9oGHDhlV7/+rVnysv77gcDodiY2OVlrZJy5YtU1FRkU6cOK6ePW+QJK1fv1579+7Vl19+ocTEQcrPz9fq1Z/L6XRqw4b1GjBggCRp3rx5cjqdysnJUffu3VVaWqq0tDRt25ahoqIiXXNNhIKDg6usf/Xqz5WYOEiS9NFHixQfH2/N8/bbbyksLFwbNqy31v3ee39SWFi49u/fp2uuidB77/1Jgwffo3nzPtC+ffsUHx+vt99+S/fcc4+2bXMFsg0b1svpdKpnzxv03nt/0rlzTu3du1eRka6a3LetXv25bDaboqOjm+plQTNYvfpz+fq2l9PpVERERJX7//73v6tPnz7W307nOa1cuVJHjzqUl3dc4eHhOn36tJYvX6Zz51z7ef/+/XTkiEPh4eFasuRj68spPDxcYWFh+uKLL9SnTx+tX79O+/btVWxsX2v5//pXlnbt2n0pvDnUs+cN2rdvr/70pz+pffv21m3u2g8ezNHq1Z/rnnvukSRr/3S/V9E2vP32WyotvaBvv02zPkcrSktL07ffpikvL8/6TE5LS9O+ffvkcDjk6+sru92uZcuWKjAwUMuXL1Ni4iDNmfOOLlwotfaXc+fO6euvv1Za2iadO+e09lk39z7n/pyVpP/8z7c1YMAA+fq2V35+vv7853kKDg7Wtm0Zio3tq/z8fG3YsF7bt2/X8ePHFR0drYsXS+Xr217Lly9Tv379JUnz5n2gAQN+oNde+73at29faR1Llnys/Px8ffHFGgUGBsput1e7/3srX08XgMrs9msrvXGulJg4SD179qxwi03Tp78gSUpL22TdOn78eEnS3r17JbneJO75zp07V2G+nyosLNyaZ9KkZzR+/HitX7++2mA3atT9CgsLk8PhqFCz6001atT9kqTY2FgNGzbMevymTZus29xuu+12SVJ4eLh1W3x8vNLSNsnhcGj8+AclVf6V5X5Os2a9aT0X920VnztaH6fznB57bIIkV9CvLoTcf//9Onr0qK699lotW7bMeu0rvif27t2rsLBwax/etCmt0vvl9ttvr/QD4Npr7ZKkwMBADRt2b6X17dy509oPHY4jklz744wZv7fWNW/ePE2aNKlSDR99tMh6Lu4a0Xa4P3uWLVtW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SJL69eunsWPH6uc//7natWun+Ph4vfLKKx6utnb33XdfvfaXixcvKjk5Wbm5uU1XVCO9++67yszM1IQJEyrdvmrVKq1fv14TJ07U4sWLNWbMmCat47HHHtPcuXM1ZswYde/eXQcPHrTuW7x4sWJjY7V48WLdc889evDBBxUfH9+k9dRXu3bt1KtXL7Vv316ZmZmeLqfRfvzjH0uS/va3v3m4Enia14YqE34Z30H9/ufyaaqdA330r//oLF8fm/6adU6/T2vaX6uobMSIEYqLi6vXY6ZMmdJE1TRMaGionnvuOW3btk07duxQv379VFZWph07dmjHjh1WkAoKCtLatWvVoUMHDRo0yMNVV2/27NnVvh5TpkzR+++/X+X2zMxMtWvXrtr7WoLMzMxa96+zZ8/qvffe05o1a7Ro0SJNnDixSS57VbGO999/X6mpqXryySd14MABde/eXX/5y1+0ceNGSdIf//hHSdIbb7zRYn48PPXUU/L399e3334rPz8/hYWFGbneamTsQPX58dMKv75vzTOVl+t//zRVuVmbG70+t6efflpff/21Nf3nP//Z2LJr0rdvX+3cubPJ14P6I1Q1wsS/FeqxuEB9lHlOknTyXJlu/osrZIUF+Oj/PdFZER18tDzbqec2FHmy1Fp99tlnlY5QXe2X4+TJkyWp3gEGV7dlyxa99dZbSk9PV79+/bRjxw6lpKRo+vTpleY7c+aMhg8frsDAQG3btk233367hyr2Dr/73e90zz331Gne/fv3a9++ffrHP/5h/AjRH//4RyUlJVW6LSkpSZmZmbr//vs1aNAgLV26tMrjrmwi9IRx48apX79+evPNN3X2rOuI/muvvabf/va3RpY/4BfvaPeX8/XN7Mf1wAcZSn6m+m0/+v1/aNWUO4ysc9KkSVq3bp32798vydXRfdKkSVqwYIHOnz9vZB1X8vPz07hx4whVLRShqhG+yy3VxkfCrVBVUb6zTLfOdwWsEH+btk3srJ6h7XT/pwXadKhp3mwNVbEP1dV+jdd0lAHmjBgxQjNmzNCZM2fUr18/zZ49W6+//roWLVqkAQMGKCcnxzoSce7cOb3zzju66aab9P3333u48sp+/ev6XYXg17/+tbZv395E1TTOuHHjNGPGjDrPP2PGDIWEhBiv41e/+pW++eYbjRs3Tnl5eZIqv2cPHTqkadOmaebMmdZjrrvuOr3xxhvGa6mPadOmaf78+fr0008lSddff70eeeQRo03YNh8fde51m2J+/LQkWf936ROvLr1u04k93+nknu90au+ORq/Lz89Pjz/+uBWopk2bprKyMr311ltat26dXnzxRb3xxhtGglVMTIw6deqkf/zjH5KkoUOHWv2eXn31VX3yySfatWtXo9cDM7x6SAUTnKVX75tVWFKu2xec1IpdTq0ZH6p3hgY3Q2VozWJjYyW5xuaZNm2aRo8erZUrV+r555/Xn/70J0VERFjzLl26tMn78DTE2rVr6z1/S+1P5T4SUR9fffWVevXqZbyWCRMmaPHixYqMjNTTrGTTlgAAC2BJREFUTz+tjz76yLqvtLRUDz30kGbPni1/f3/dd999Wrx4sVJSUozXUVc333yzCgoKKnWWnzBhgjIyMoyfPNTlilAV8+On1aXXbdZ9fX70pHZ/Ob/R65k4caKuvfZaa7/4y1/+IpvNptDQUO3fv99q/jXhoYce0nfffWf9feONN1p96A4fPqyf/exnRtZTEWcDNhyhqpFGryjQuJiAOs371JrTsv/3cT3ZL7Be64iIiNCPfvQj6+977723Xo9vKSoGAal1P4+Kr0fXrl2NLv/gwYMaP3689XdZWZlKS0sVFxdn/UtNvXzppPPnz2vcuHH1Xk9beT0kNenrIUlpaWn1fsw333yjH/zgB8Zr2b9/v5555hktXbpUISEheuuttyrdHxcXpz/+8Y/6+c9/rpMnT9a52bIp9O/fX3feeafmzZtn3fbaa69pxowZ+uKLLxQUFKTXXntNzzzzjH71q181en3Zf/uz1eyX/Ex81X+TE5Sb9b+NWsdTTz2ldevWacaMGQoNDdVvfvMbnTp1Sn/4wx/09NNPKzw8XPv379e6dev01FNPNepsuldffVUzZ86sNED1Nddcoy+//FKS9OGHH2rmzJl69dVXG/WcYA6hqpE2H76gBSM7qUdIu6vO+7vEjnL88hrtPF7zCO7VycvL06233qopU6Zow4YNrbYtPS8vT5mZmbLb7a3+eVR8PT7++GOjy1+2bFmVgOPrW3tL/dXur4779Wjt+5WkJn09JFcftvoKCgpSYWGh8VokV7AaPHhwlUDllpubq/nz52vr1q1Nsv662r59uxYsWCBJuuOOO6w+VOXl5brrrrv00EMP6be//a3sdrux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)

