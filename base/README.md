
# CourseApp: Assignment 2

## Authors
* Ron Yitzhak
* Aviad Shiber

## Notes

### Implementation Summary
#### Storage Layer
    -UserStorage, ChannelStorage, TokenStorage
        -Wrappers for read and write operations
        -each username is mapped to unique user id
        -each channel name is mapped to unique channel id
        -each <property> of user/channel represented by key-value pair in the form: <id>_<propertyName> -> <propertyValue>
    -StatisticsStorage
        -contains information about user's count and channel's count
    -MessageStorage
        -contains information about message as data class
***
#### Managers layer
    -UserManager: User Manager is the entity that responsible to all the actions related to user
        -generate user id using Generator
        -each user has id, name, status, privilege, and channels list
        -contains avl tree of users, and responsible to update it
            -each user has a representing node in the tree
            -the key of this node is <channels count><user id> (takes care to primary and secondary required order)
    -TokenManager: Token Manager is the entity that responsible to all the actions related to tokens in the system
        -mapping from token to user id
    -ChannelManager: Channel Manager is the entity that responsible to all the actions related to channels in the system
        -generate channel id using Generator
        -each channel has id, name, active users counter and 2 lists: members list and operator list (contains user ids)
        -contains 3 avl trees of channels, and responsible to update them
            -each channel has a representing node in the trees
                -the key of the node in the first tree is <number of users in channel><channel id>
                -the key of the second node is <number of active users in channel><channel id>
                -the key of the third node is <channelId>_<messages>
    -StatisticsManager(used by User and Channel managers): API to get/set statistics values
    -MessageManager: 
        -Api to manage messages across the system.
        -Uses the same id generator for users inorder to observe if message is new.
        -Uses AVL tree to store broadcast messages

*** 

#### App Layer
    -implements all course app logic
    -Responsible to the communication between Users and Channels
    (add/remove channel from user's list, add/remove user from channel's lists)
    - adding users as observables to callbacks of messages

### Notes
* we used Interfaces as much as possible so the app will be flexible  as much as possible
* we used *Guice* as our Dependency injection framework and we used the special annotations like *@Singleton* on the Manager & *StorageLayer*  since there is no need more than one instance of them, each Storage entity is injected once by using the *SecureStorageFactory* in the *LibraryModule* which provides the different SecureStorages.
* we used the proxy pattern to add *LRU cache* to the *SecureStorage* to optimize performance of immediate actions (like *AVL tree access* and *etc...*)- can be found on *LibraryModule*
* we used completable future as much as possible (all the project except for the AVL TREE) and design it to work in parallel whenever possible
* secure avl tree will receive now a byte array key for root key, default one will be ROOT_KEY.toByteArray() to make sure the other trees remained the same.

### Testing Summary
- Unit testing:
  - All managers
  - AvlTree
  - CourseApp
  - CourseAppStatistics
### Difficulties
- refactoring to CompletableFuture was really a challenge , we chained the monads all away the project except
for one place that broke us because it took too much time from us to redsign it inorder to work- THE AVL TREE

- Understanding Monads- it is still not 100% clear to us, we hope the dry part will suffice

### Feedback
- As can be noticed by the FAQ there were lots of holes in this assignment and many place for personal interpetation , we hope this can be miminzed in the next assignment
- The staff was very helpfull and responsive , that helped a lot!
