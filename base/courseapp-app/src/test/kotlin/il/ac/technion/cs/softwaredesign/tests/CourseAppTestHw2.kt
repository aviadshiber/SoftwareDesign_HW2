import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import il.ac.technion.cs.softwaredesign.tests.CourseAppTestModule
import il.ac.technion.cs.softwaredesign.tests.isTrue
import il.ac.technion.cs.softwaredesign.tests.runWithTimeout
import io.github.vjames19.futures.jdk8.ImmediateFuture
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.concurrent.CompletableFuture

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class CourseAppTestHw2 {
    private val injector = Guice.createInjector(CourseAppTestModule())
    private val courseApp = injector.getInstance<CourseApp>()
    private val courseAppStatistics = injector.getInstance<CourseAppStatistics>()
    private val courseAppInitializer = injector.getInstance<CourseAppInitializer>()
    private val messageFactory = injector.getInstance<MessageFactory>()


    init {
        injector.getInstance<CourseAppInitializer>().setup().join()
    }

    @Test
    fun `Broadcast received only after users listen`() {
        var ronNoCalls=0
        var aviadNoCall=0
        val ronCallback :ListenerCallback = {
            source, message -> ImmediateFuture { ronNoCalls++; println("sent from $source"+String(message.contents))}
        }
        val aviadCallback :ListenerCallback = {
            source, message -> ImmediateFuture { aviadNoCall++; println("sent from $source"+String(message.contents))}
        }

        val aviad=courseApp.login("aviad","shiber").join()
        val ron=courseApp.login("ron","123").join()
        messageFactory.create(MediaType.TEXT, "no one should receive it".toByteArray())
                .thenCompose {courseApp.broadcast(aviad,it)  }
                .thenCompose {  courseApp.addListener(ron,ronCallback) }
                .thenCompose { courseApp.addListener(aviad,aviadCallback) }
                .thenCompose { messageFactory.create(MediaType.TEXT, "aviad and ron should get this".toByteArray())  }
                .thenCompose { courseApp.broadcast(aviad,it) }.join()


        assertThat( ronNoCalls , equalTo(1))
        assertThat( aviadNoCall , equalTo(1))

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