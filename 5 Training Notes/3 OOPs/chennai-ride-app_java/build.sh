#!/usr/bin/env bash
# Compile with only the JDK, then run.
#   ./build.sh               start-up menu (demo / interactive / self-check)
#   ./build.sh --demo        the scripted simulated day          (old form: ./build.sh demo)
#   ./build.sh --check       SelfCheck                           (old form: ./build.sh check)
#   ./build.sh --cli         interactive mode
#   ./build.sh all           the day, then SelfCheck
# Replay a session:  ./build.sh --cli < walkthroughs/01_egmore_to_tambaram.txt
set -e
cd "$(dirname "$0")"

rm -rf out
mkdir -p out
find src -name '*.java' > out/sources.txt
javac -encoding UTF-8 --release 17 -d out @out/sources.txt
echo "Compiled $(wc -l < out/sources.txt | tr -d ' ') source files into out/" >&2

case "$1" in
  --demo|demo)   java -cp out com.ridehailing.app.Launcher --demo ;;
  --check|check) java -cp out com.ridehailing.app.Launcher --check ;;
  --cli|cli)     shift; java -cp out com.ridehailing.app.Launcher --cli "$@" ;;
  all)           java -cp out com.ridehailing.app.Launcher --demo && java -cp out com.ridehailing.app.Launcher --check ;;
  "")            java -cp out com.ridehailing.app.Launcher ;;
  *)             echo "Unknown option $1 (use --demo, --check, --cli or nothing)" >&2; exit 2 ;;
esac
