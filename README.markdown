SBT build
=======================

All actions done on root project is performed against all subprojects

    sbt> update
    sbt> clean
    sbt> test
    ...

To switch to a dedicated project

    sbt> project technbolts-util
	sbt> ...

GIT
======================

    git push origin master