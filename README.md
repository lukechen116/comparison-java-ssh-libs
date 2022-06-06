* Fork from https://github.com/sparsick/comparison-java-ssh-libs.git
* Add StrictHostKeyChecking, useCompression, connect port, and fix some type conversion
# Performance Test
* Download 1MB File
1. sshj average 544 ms
2. Apache Commons VFS2 576 ms
3. jsch (mwiede) average 614 ms

* Download 15MB File
1. sshj average 2321 ms
2. Apache Commons VFS2 2346 ms
3. jsch (mwiede) average 2475 ms

# Other
1. sshj is the best scp download performance, but tiny difference
2. jsch (JCraft) is very popular, and a lot of porject include this.
3. jsch (JCraft) last release version 0.1.55 at Nov 26, 2018, that too long fix or release new version.
4. jsch (mwiede) is fork from jsch (JCraft), and last release version 0.2.1 at Apr 26, 2022.
5. Apache Commons VFS2 last release version 2.9.0 at Jul 20, 2021, that have some vulnerabilities from old Apache Log4j2.
6. sshj last release version 0.33.0 at Apr 20, 2022.

# Reference
* https://github.com/sparsick/comparison-java-ssh-libs.git
* jsch (JCraft) http://www.jcraft.com/jsch/
* jsch (mwiede) https://github.com/mwiede/jsch
* sshj https://github.com/hierynomus/sshj
* Apache Commons VFS2 https://commons.apache.org/proper/commons-vfs/
