#JBoss Module Creator

Very simple script to create JBoss Modules.  Anything more complicated would be
silly since you could just write the XML at that point.  Perhaps this could be
used to start a module and then modify the XML from there.

Example usage:

    java -jar module-creator.jar \
      --name com.redhat.gss \
      --jars some.jar:another.jar \
      --deps org.apache.cxf,javax.api
