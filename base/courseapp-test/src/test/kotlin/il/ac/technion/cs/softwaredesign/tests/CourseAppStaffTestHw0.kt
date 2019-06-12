//package il.ac.technion.cs.softwaredesign.tests
//
//import com.authzee.kotlinguice4.getInstance
//import com.google.inject.Guice
//import com.natpryce.hamkrest.absent
//import com.natpryce.hamkrest.assertion.assertThat
//import com.natpryce.hamkrest.present
//import il.ac.technion.cs.softwaredesign.CourseApp
//import il.ac.technion.cs.softwaredesign.CourseAppInitializer
//import il.ac.technion.cs.softwaredesign.CourseAppModule
//import il.ac.technion.cs.softwaredesign.exceptions.InvalidTokenException
//import java.time.Duration.ofSeconds
//import org.junit.jupiter.api.*
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class CourseAppStaffTestHw0 {
//
//    private val SMALL_TEST_FILE_NAME: String = "small_test.csv"
//
//    private val LARGE_TEST_FILE_NAME: String = "large_test.csv"
//
//    private val injector = Guice.createInjector(CourseAppModule(), OurSecureStorageModule())
//
//    private var courseAppInitializer: CourseAppInitializer
//    private var courseApp: CourseApp
//
//    init {
//        this.courseAppInitializer = injector.getInstance<CourseAppInitializer>()
//        this.courseAppInitializer.setup()
//        this.courseApp = injector.getInstance<CourseApp>()
//    }
//
//    @Nested
//    @TestInstance(TestInstance.Lifecycle.PER_METHOD)
//    inner class SanityTest {
//
//        init {
//            val storageFactory = injector.getInstance<SecureHashMapStorageFactoryImpl>()
//            storageFactory.clear()
//            courseAppInitializer = injector.getInstance<CourseAppInitializer>()
//            courseAppInitializer.setup()
//            courseApp = injector.getInstance<CourseApp>()
//        }
//
//        @Test
//        fun `basic login - single user`() {
//            val token = courseApp.login("User1", "password").join()
//            assertTrue(courseApp.isUserLoggedIn(token, "User1").join() == true)
//        }
//
//        @Test
//        fun `basic logout - single user`() {
//            val token = courseApp.login("User1", "password").join()
//            val token2 = courseApp.login("User2", "password").join()
//            courseApp.logout(token).join()
//            assertTrue(courseApp.isUserLoggedIn(token2, "User1").join() == false)
//        }
//
//        @Test
//        fun `basic logout - single user(exception case)`() {
//            val token = courseApp.login("User1", "password").join()
//            courseApp.logout(token).join()
//            assertThrows<InvalidTokenException>({ courseApp.isUserLoggedIn(token, "User1").joinException() })
//        }
//    }
//
//
//    @Nested
//    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
//    inner class MainTest {
//
//        fun initCourseApp(fileName: String): Map<String, String> {
//            return buildUserIdToTokenMap(courseApp, getPathOfFile(fileName))
//        }
//
//        @BeforeEach
//        fun clearDb() {
//            val storageFactory = injector.getInstance<SecureHashMapStorageFactoryImpl>()
//            storageFactory.clear()
//            courseAppInitializer = injector.getInstance<CourseAppInitializer>()
//            courseAppInitializer.setup()
//            courseApp = injector.getInstance<CourseApp>()
//        }
//
//        @Test
//        fun `after login, user is logged in`() {
//
//            val tokenMap = initCourseApp(SMALL_TEST_FILE_NAME)
//            assertWithTimeout({
//
//                assertThat(courseApp.isUserLoggedIn(tokenMap["oem1ec8wg7"] as String, "oem1ec8wg7").join(), present(isTrue))
//                assertThat(courseApp.isUserLoggedIn(tokenMap["bmz3wmx6wt"] as String, "bmz3wmx6wt").join(), present(isTrue))
//
//
//            })
//        }
//
//        @Test
//        fun `for non-existent user isUserLoggedIn returns null`() {
//
//            val tokenMap = initCourseApp(SMALL_TEST_FILE_NAME)
//            assertWithTimeout(
//                    {
//
//                        assertThat(courseApp.isUserLoggedIn(tokenMap["oem1ec8wg7"] as String, "User343").join(), absent())
//                    }
//            )
//        }
//
//        @Test
//        fun `for existent user that logged out, isUserLoggedIn returns false`() {
//            val tokenMap = initCourseApp(SMALL_TEST_FILE_NAME)
//            assertWithTimeout(
//                    {
//
//                        assertThat(courseApp.isUserLoggedIn(tokenMap["oem1ec8wg7"] as String, "egqgtwnm9r").join(), present(isFalse))
//                        assertThat(courseApp.isUserLoggedIn(tokenMap["oem1ec8wg7"] as String, "fkhzl5cxk4").join(), present(isFalse))
//                    }
//            )
//        }
//
//        @Test
//        fun `for logged out user, login succeeds`() {
//            val tokenMap = initCourseApp(SMALL_TEST_FILE_NAME)
//            assertWithTimeout(
//                    {
//
//                        val token = courseApp.login("egqgtwnm9r", "hkriqza8rf").join()
//                        assertThat(courseApp.isUserLoggedIn(token, "egqgtwnm9r").join(), present(isTrue))
//                    }
//            )
//        }
//
//        @Test
//        fun `for logged out user, login fails with wrong password`() {
//            val tokenMap = initCourseApp(SMALL_TEST_FILE_NAME)
//            assertWithTimeout(
//                    {
//
//                        assertThrows<IllegalArgumentException> { courseApp.login("egqgtwnm9r", "nbjssn5grc").joinException() }
//
//                    }
//            )
//
//        }
//
//        @Test
//        fun `an authentication token is invalidated after logout`() {
//
//            val tokenMap = initCourseApp(SMALL_TEST_FILE_NAME)
//            assertThrows<IllegalArgumentException> {
//                runWithTimeout(ofSeconds(10)) {
//
//                    courseApp.isUserLoggedIn(tokenMap["egqgtwnm9r"] as String, "egqgtwnm9r").joinException()
//                }
//            }
//        }
//
//        @Test
//        fun `logout with invalid token`() {
//            val tokenMap = initCourseApp(SMALL_TEST_FILE_NAME)
//            assertWithTimeout(
//                    {
//
//                        assertThrows<IllegalArgumentException> { courseApp.logout(tokenMap["au6xvemv3a"] as String).joinException() }
//                        assertThrows<IllegalArgumentException> { courseApp.logout("MagicalToken").joinException() }
//                    }
//            )
//
//        }
//
//
//        @Test
//        fun `login with user who's already logged in`() {
//
//            val tokenMap = initCourseApp(LARGE_TEST_FILE_NAME)
//            assertWithTimeout(
//                    {
//
//                        assertThrows<IllegalArgumentException>
//                        { courseApp.login("pvdfu81uma", "khtevw").joinException() }
//                        assertThrows<IllegalArgumentException>
//                        { courseApp.login("kiu9t1ucva", "g9xj2t").joinException() }
//                    }
//            )
//
//        }
//
//        @Test
//        fun `check logged in users after init course app again`() {
//            val tokenMap = initCourseApp(SMALL_TEST_FILE_NAME)
//            assertWithTimeout(
//                    {
//                        courseApp = injector.getInstance<CourseApp>()
//                        val validToken = tokenMap["oem1ec8wg7"] as String
//                        assertThat(courseApp.isUserLoggedIn(validToken, "oem1ec8wg7").join(), present(isTrue))
//                        assertThat(courseApp.isUserLoggedIn(validToken, "bmz3wmx6wt").join(), present(isTrue))
//
//                    }
//            )
//        }
//
//        @Test
//        fun `check for logins after reinitialization`() {
//            val tokenMap = initCourseApp(LARGE_TEST_FILE_NAME)
//            assertWithTimeout(
//                    {
//                        val storageFactory = injector.getInstance<SecureHashMapStorageFactoryImpl>()
//                        storageFactory.clear()
//                        courseApp = injector.getInstance<CourseApp>()
//                        val validToken = courseApp.login("User8", "password").join()
//                        assertThat(courseApp.isUserLoggedIn(validToken, "vgl9xcjbfj").join(), absent())
//
//
//                    }
//            )
//        }
//
//    }
//
//}