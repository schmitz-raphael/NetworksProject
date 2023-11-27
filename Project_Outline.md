# Networks 1 Project

### Project - Implementing an efficient many to many communication protocol over UDP

## 1. Goal

The goal of this project is to implement an efficient protocol allowing the
communication among multiple participants. For our project, you have to
create up to 10 clients, where each client is a separate process, that connect
to one server that stores a file that will be further downloaded by all clients
at the same time.

### 1.1 Program

The tool to initiate the clients will be started as follows from the command
line:

`toolname`, `id process`, `number of processes`, `filename`, `probability`, `protocol`,
`window size`

- `id process`: is the number of the current process (for instance 2)
- `number of processes`: total number of processes that will join this com-
munication (for instance 10)
- `filename`: name of file to be send to each client that is connected to this
server
- `probability`: probability of an UDP send not to be successful - this is
to simulate network errors and thus retransmissions.
- `protocol`: Go-back-N over UDP (optional also selective repeat is possi-
ble).
- `window size`: the size of the window for Go-back-N

Once started, each process will wait until all the processes have joined
the session and then start downloading a file (provided by the filename) from
the server.

You have to use only UDP for your program. You will need to provide a wrapper function to the UDP send function that ensures that the success of really sending data is (1-probability). In case failures occur, you will
need to ensure that retransmissions are handled correctly. After execution
your program should have received correctly the transmitted files from all
the processes and report on the display the total number of bytes/packets
received, total number of bytes/packets sent and how many retransmissions
(received/sent) were done.

## 1.2 Protocol and data transfer
Each client is using Go-back-N with a respective windows size in the range
1-10. Before the next datagram can be downloaded, all (!) clients need to
have received the former datagram. The sliding window is advanced only if
all clients have received the packet.

Hint: A synchronisation needs to happen between the processes. Please
note that the sending should be done to all clients from the same and only
server process - you’re not allowed to start a server instance for each client.
You have to use the same window scheme (Go-back (n)) for all clients, but for
each sent packet maintain individual timers (one timer per client/process).

## 2. Groups

You can work alone or together with another person (max. group size is two).

Please let me know your group until 30.11.2023 by writing a mail to
stefan.hommes@uni.lu. The mail needs to contain your first name, last
name, and student id. In case that you work in a group, please send only
one mail with the information of both of you.

## 3. What to hand in

In order to pass the project, you need to demonstrate your solution in the
classroom and you have to write a project report.

## 3.1 Demo

You will have to implement the solution in either Java or Python and demon-
strate the running program in the class room. The demo will happen in our
classroom on the 19 January 2023 at 16 o’clock. Please be well prepared
since time is limited for each student/group.

## 3.2 Report

In addition every student/group has to write a report and submit the
report + code before the deadline: **11 January 2023**. The title page of the
report needs to contain your **first name, last name, and student id**. In
case of a group, also the same information about the second person.

Your report needs to address the following topics:

1. General architecture covering:

    (a) Bootstrapping: this means sending/receiving starts only when all
processes have joined and somehow the IP addresses of them are
known to the other processes.

    (b) Details on how you simulated the loss and the protocols (Go-back-
n)

    (c) Details on how to verify that the received file has no corrupted
content

    (d) Details on how you can achieve savings in bandwidth and better
delivery times

    (e) Details on parameters choice for Go-back-N (timeouts, window
size, etc)

2. Performance evaluation:

    (a) Graphs/tables showing the impact of different probabilities (0.05,
0.10 , 0.15, 0.20, 0.30 and 0.40 and protocol (timeout, window
size) on the total time (delay) and bandwidth.
3

    (b) These graphs should cover at least 2 scenarios. Scenario 1: two
processes download 1 GB files and scenario 2: 10 processes down-
load 10 files in a sequence, et least 50 MB per file.

3. Wireshark screenshots:

    (a) Show statistics only for the communication of the tool. This
should include statistics about endpoints (IP adresses, ports),
#bytes exchanged, average time for downloading a segment. Hint:
You can use display filters for this task.


Important remark: All the material that you use and was not developed
by you (code, text, etc.) needs to be quoted and cited correctly in your
report.