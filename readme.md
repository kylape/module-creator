#JBoss Module Creator

Very simple script to create JBoss Modules.  Example usage:

    java -jar module-creator.jar \
      --name com.redhat.gss \
      --jars some.jar:another.jar \
      --deps org.apache.cxf,javax.api

