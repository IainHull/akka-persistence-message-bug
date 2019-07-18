# akka-persistence-message-bug

Small repository to reproduce a message bug in akka-persistence-typed where commands are lost when snapshots are created.

[https://github.com/akka/akka/issues/27381]

When simulating large amounts of traffic in an application built on top of akka-persistence-typed it seems to loose commands sent to
persistent actors when they are storing snapshots.

Using a logging interceptor around the persistent actor all messages entering the behavior are logged. The persistent actor also logs
all commands processed by the `commandHandler` and each time the function sent to `snapshotWhen` returns true. With these we can see
some messages sent to the persistent actor and logged by the interceptor while the snapshot is being written never make it to the
`commandHandler`.

* The mainline of this sample application is `io.github.iainhull.bug.Application`, run this it will stop once it has reproduced the bug.
* I have only seen it fail on an ask not a direct send.
* This sample application uses a `TestActor` to sends 100s of `RegisterValue` commands to the `PersistentActor`.
* After each batch of 100 it polls the `PersistentActor` with `GetState` requests to verified that all values have been received.
* It stops if any of the values sent in `RegisterValue` are missing or the `GetState` ask times out.

Here is a sample from logs on my machine where an ask command `GetState` times out:

```text
019-07-18 22:50:03.931Z DEBUG [io.github.iainhull.bug.TestActor$] Intercepted message: Storm(0)
2019-07-18 22:50:03.931Z INFO [io.github.iainhull.bug.TestActor$] Starting Storm 0
2019-07-18 22:50:03.933Z INFO [io.github.iainhull.bug.TestActor$] Stopping Storm 0
2019-07-18 22:50:03.933Z DEBUG [io.github.iainhull.bug.TestActor$] Intercepted message: TriggerVerify(0,0)
2019-07-18 22:50:03.934Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(0)
2019-07-18 22:50:03.935Z INFO [i.g.iainhull.bug.PersistentActor$] Received command RegisterValue(0)
2019-07-18 22:50:03.939Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(1)
2019-07-18 22:50:03.939Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(2)
2019-07-18 22:50:03.939Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(3)
2019-07-18 22:50:03.940Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(4)
2019-07-18 22:50:03.940Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(5)
2019-07-18 22:50:03.940Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(6)
2019-07-18 22:50:03.940Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(7)
2019-07-18 22:50:03.940Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(8)
2019-07-18 22:50:03.940Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(9)
2019-07-18 22:50:03.941Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: RegisterValue(10)

...

2019-07-18 22:50:04.136Z INFO [i.g.iainhull.bug.PersistentActor$] Received command RegisterValue(8)
2019-07-18 22:50:04.139Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessagesSuccessful
2019-07-18 22:50:04.139Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessageSuccess(PersistentImpl(ValueRegistered(8),9,persistence,,false,null,e03f4ccb-81eb-4f5f-9510-842a4817935a),1)
2019-07-18 22:50:04.139Z INFO [i.g.iainhull.bug.PersistentActor$] Received command RegisterValue(9)

2019-07-18 22:50:04.139Z INFO [i.g.iainhull.bug.PersistentActor$] Lets snapshot 10 ValueRegistered(9) 10

2019-07-18 22:50:04.143Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessagesSuccessful
2019-07-18 22:50:04.144Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessageSuccess(PersistentImpl(ValueRegistered(9),10,persistence,,false,null,e03f4ccb-81eb-4f5f-9510-842a4817935a),1)
[WARN] [SECURITY][07/18/2019 23:50:04.149] [app-akka.persistence.dispatchers.default-plugin-dispatcher-7] [akka.serialization.Serialization(akka://app)] Using the default Java serializer for class [io.github.iainhull.bug.PersistentActor$State] which is not recommended because of performance implications. Use another serializer or disable this warning using the setting 'akka.actor.warn-about-java-serializer-usage'
2019-07-18 22:50:04.203Z DEBUG [io.github.iainhull.bug.TestActor$] Intercepted message: DoVerify(0,0)
2019-07-18 22:50:04.203Z INFO [io.github.iainhull.bug.TestActor$] Starting verify 0, 0

2019-07-18 22:50:04.209Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: GetState(Actor[akka://app/temp/$b#0])

2019-07-18 22:50:04.241Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: SaveSnapshotSuccess(SnapshotMetadata(persistence,10,1563490204147))

2019-07-18 22:50:04.245Z INFO [i.g.iainhull.bug.PersistentActor$] Received signal SnapshotCompleted(SnapshotMetadata(persistence,10,1563490204147))

2019-07-18 22:50:04.245Z INFO [i.g.iainhull.bug.PersistentActor$] Received command RegisterValue(10)
2019-07-18 22:50:04.254Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessagesSuccessful
2019-07-18 22:50:04.255Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessageSuccess(PersistentImpl(ValueRegistered(10),11,persistence,,false,null,e03f4ccb-81eb-4f5f-9510-842a4817935a),1)

...

2019-07-18 22:50:04.605Z INFO [i.g.iainhull.bug.PersistentActor$] Received command RegisterValue(97)
2019-07-18 22:50:04.610Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessagesSuccessful
2019-07-18 22:50:04.610Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessageSuccess(PersistentImpl(ValueRegistered(97),98,persistence,,false,null,e03f4ccb-81eb-4f5f-9510-842a4817935a),1)
2019-07-18 22:50:04.610Z INFO [i.g.iainhull.bug.PersistentActor$] Received command RegisterValue(98)
2019-07-18 22:50:04.614Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessagesSuccessful
2019-07-18 22:50:04.614Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessageSuccess(PersistentImpl(ValueRegistered(98),99,persistence,,false,null,e03f4ccb-81eb-4f5f-9510-842a4817935a),1)
2019-07-18 22:50:04.614Z INFO [i.g.iainhull.bug.PersistentActor$] Received command RegisterValue(99)
2019-07-18 22:50:04.614Z INFO [i.g.iainhull.bug.PersistentActor$] Lets snapshot 100 ValueRegistered(99) 100
2019-07-18 22:50:04.619Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessagesSuccessful
2019-07-18 22:50:04.620Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessageSuccess(PersistentImpl(ValueRegistered(99),100,persistence,,false,null,e03f4ccb-81eb-4f5f-9510-842a4817935a),1)
2019-07-18 22:50:04.622Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: SaveSnapshotSuccess(SnapshotMetadata(persistence,100,1563490204620))
2019-07-18 22:50:04.622Z INFO [i.g.iainhull.bug.PersistentActor$] Received signal SnapshotCompleted(SnapshotMetadata(persistence,100,1563490204620))
2019-07-18 22:50:04.622Z INFO [i.g.iainhull.bug.PersistentActor$] Received command RegisterValue(100)
2019-07-18 22:50:04.625Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessagesSuccessful
2019-07-18 22:50:04.625Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted message: WriteMessageSuccess(PersistentImpl(ValueRegistered(100),101,persistence,,false,null,e03f4ccb-81eb-4f5f-9510-842a4817935a),1)

2019-07-18 22:50:19.225Z INFO [io.github.iainhull.bug.TestActor$] Ask failure java.util.concurrent.TimeoutException: Ask timed out on [Actor[akka://app/user/persistence#-163712343]] after [15000 ms]. Message of type [io.github.iainhull.bug.PersistentActor$GetState]. A typical reason for `AskTimeoutException` is that the recipient actor didn't send a reply.

2019-07-18 22:50:19.225Z DEBUG [io.github.iainhull.bug.TestActor$] Intercepted message: Stop
2019-07-18 22:50:19.226Z INFO [io.github.iainhull.bug.TestActor$] Stop
2019-07-18 22:50:19.228Z DEBUG [io.github.iainhull.bug.TestActor$] Intercepted signal: PostStop
2019-07-18 22:50:19.231Z DEBUG [io.github.iainhull.bug.Application$] Intercepted signal: Terminated(Actor[akka://app/user/test#391377391])
2019-07-18 22:50:19.235Z DEBUG [i.g.iainhull.bug.PersistentActor$] Intercepted signal: PostStop
2019-07-18 22:50:19.236Z INFO [i.g.iainhull.bug.PersistentActor$] Received signal PostStop
2019-07-18 22:50:19.237Z DEBUG [io.github.iainhull.bug.Application$] Intercepted signal: PostStop

```

* 100 `ValueRegistered` events are processed in less than one second.
* At 22:50:04.139Z `PersistentActor` decides to write a snapshot `Lets snapshot 10 ValueRegistered(9) 10`
* At 22:50:04.209Z `PersistentActor` logging interceptor receives the next message `Intercepted message: GetState`. This is a request
  using the ask pattern.
* At 22:50:04.245Z `PersistentActor` completes snapshot `Received signal SnapshotCompleted(SnapshotMetadata(persistence,10,1563490204147))`
* At 22:50:19.225Z 15 seconds later the `TestActor` gets ``Ask failure java.util.concurrent.TimeoutException: Ask timed out on [Actor[akka://app/user/persistence#-163712343]] after [15000 ms]. Message of type [io.github.iainhull.bug.PersistentActor$GetState]. A typical reason for `AskTimeoutException` is that the recipient actor didn't send a reply.`
* Every other `Intercepted message` log in the `PersistentActor` has a matching `Received command` log except this one

The command handler never got this message.