package in.aicon.pocket;
/** Public OAuth client identifier, not a secret. Optional Google access remains a development preview. */
public final class CloudConfig {
 private CloudConfig(){}
 // Opt-in account UI is enabled in this development candidate; live Google/Drive device QA remains required.
 public static final boolean ACCOUNT_UI_ENABLED=true;
 public static final String WEB_CLIENT_ID="859517752237-v5g6jihopv1a0nm3ap6tupon28lboubs.apps.googleusercontent.com";
 public static final String DRIVE_SCOPE="https://www.googleapis.com/auth/drive.appdata";
}
