package android.encryptor4j.org.encryptor;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.encryptor4j.android.encryptor.MainActivity;
import org.encryptor4j.android.encryptor.R;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApplicationTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void clearKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            keyStore.deleteEntry(aliases.nextElement());
        }
    }

    @Test
    public void addAndRemoveFirstKey() {
        onView(withId(R.id.alias)).perform(typeText("Test key"));
        onView(withId(R.id.add)).perform(click());

        onView(withId(android.R.id.list))
                .check(matches(hasDescendant(withText("Test key"))));

        onView(allOf(withParent(withId(android.R.id.list)), withText("Test key"))).perform(longClick());

        onView(withText("Delete")).perform(click());

        onView(withId(android.R.id.list))
                .check(matches(not(hasDescendant(withText("Test key")))));
    }
}
