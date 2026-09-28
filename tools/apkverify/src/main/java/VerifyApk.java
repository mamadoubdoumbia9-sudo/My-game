import com.android.apksig.ApkVerifier;
public class VerifyApk {
  public static void main(String[] a) throws Exception {
    ApkVerifier.Result r = new ApkVerifier.Builder(new java.io.File(a[0])).build().verify();
    System.out.println("verified: " + r.isVerified());
    if (!r.isVerified()) { r.getErrors().forEach(e -> System.out.println("error: " + e)); System.exit(1); }
  }
}
