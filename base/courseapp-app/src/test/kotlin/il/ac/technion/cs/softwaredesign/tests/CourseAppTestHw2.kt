import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.CourseApp
import il.ac.technion.cs.softwaredesign.CourseAppInitializer
import il.ac.technion.cs.softwaredesign.CourseAppStatistics
import il.ac.technion.cs.softwaredesign.tests.CourseAppTestModule
import il.ac.technion.cs.softwaredesign.tests.isTrue
import il.ac.technion.cs.softwaredesign.tests.runWithTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class CourseAppTestHw2 {
    private val injector = Guice.createInjector(CourseAppTestModule())
    private val courseApp = injector.getInstance<CourseApp>()
    private val courseAppStatistics = injector.getInstance<CourseAppStatistics>()
    private val courseAppInitializer = injector.getInstance<CourseAppInitializer>()

    init {
        courseAppInitializer.setup()
    }

    @Test
    fun `after login, a user is logged in`() {
        val token = courseApp.login("gal", "hunter2")
                .thenCompose { courseApp.login("imaman", "31337") }
                .thenCompose { courseApp.login("matan", "s3kr1t") }
                .join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) { courseApp.isUserLoggedIn(token, "gal").join() },
                present(isTrue))
    }
}