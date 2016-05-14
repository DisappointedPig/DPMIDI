# DPMIDI
Simple Android RTP MIDI

developed using android studio 1.5.1, updated for 2.1.1 (don't forget to sync project with gradle after fresh clone)

provides basic RTP (network) midi for android devices - compatable with Apple MIDI 

- works with API 8+ (2.2/Froyo)
- uses apple bonjour API 16+ (4.1.x/Jelly Bean)

### what DPMIDI does

- connect to other devices through apple network midi
- send and receive simple midi commands
- dispatch received midi over greenrobot eventbus

### what DPMIDI doesn't do

- doesn't handle realtime streams
- doesn't handle bitrate receive limit
- doesn't handle receiver feedback
- doesn't handle rtp journal data
 

---

created using the following efforts of others:

- https://tools.ietf.org/html/rfc4695
- http://john-lazzaro.github.io//rtpmidi/
- https://github.com/jdachtera/node-rtpmidi
- http://goodliffe.blogspot.com/2010/10/using-coremidi-in-ios-example.html
- https://cs.fit.edu/code/svn/cse2410f13team7/wireshark/epan/dissectors/packet-applemidi.c
- http://www.cs.columbia.edu/~hgs/rtp/drafts/draft-ietf-avt-mwpp-midi-rtp-05.txt
- http://www.tobias-erichsen.de/software/rtpmidi.html

many thanks to wireshark and tobias erichsen
