package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.KotlinModule

import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.messages.MessageFactoryImpl
import il.ac.technion.cs.softwaredesign.messages.MessageImpl


class CourseAppModule : KotlinModule() {
    override fun configure() {
        bind<CourseAppInitializer>().to<CourseAppInitializerImpl>()
        install(LibraryModule())
        bind<CourseAppStatistics>().to<CourseAppStatisticsImpl>()
        bind<CourseApp>().to<CourseAppImpl>()
        bind<MessageFactory>().to<MessageFactoryImpl>()
        bind<Message>().to<MessageImpl>()
    }

}