import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphClient.Transaction;
import io.dgraph.DgraphGrpc;
import io.dgraph.DgraphGrpc.DgraphBlockingStub;
import io.dgraph.DgraphProto.Mutation;
import io.dgraph.DgraphProto.Operation;
import io.dgraph.DgraphProto.Response;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AppTour {
  private static final String TEST_HOSTNAME = "localhost";
  private static final int TEST_PORT = 9080;

  public static void main(final String[] args) {
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress(TEST_HOSTNAME, TEST_PORT).usePlaintext(true).build();
    DgraphBlockingStub blockingStub = DgraphGrpc.newBlockingStub(channel);
    DgraphClient dgraphClient = new DgraphClient(Collections.singletonList(blockingStub));

    // Initialize
    dgraphClient.alter(Operation.newBuilder().setDropAll(true).build());

    // Set schema
    String schema = "name: string @index(term) @lang .\n" + "age: int @index(int) .\n" + "friend: uid @count .";
    Operation op = Operation.newBuilder().setSchema(schema).build();
    dgraphClient.alter(op);

    Gson gson = new Gson(); // For JSON encode/decode

    Transaction txn = dgraphClient.newTransaction();
    try {
      // Create data
      String triples = "    _:michael <name> \"Michael\" .\n" +
              "    _:michael <age> \"39\" .\n" +
              "    _:michael <friend> _:amit .\n" +
              "    _:michael <friend> _:sarah .\n" +
              "    _:michael <friend> _:sang .\n" +
              "    _:michael <friend> _:catalina .\n" +
              "    _:michael <friend> _:artyom .\n" +
              "    _:michael <owns_pet> _:rammy .\n" +
              "\n" +
              "    _:amit <name> \"अमित\"@hi .\n" +
              "    _:amit <name> \"অমিত\"@bn .\n" +
              "    _:amit <name> \"Amit\"@en .\n" +
              "    _:amit <age> \"35\" .\n" +
              "    _:amit <friend> _:michael .\n" +
              "    _:amit <friend> _:sang .\n" +
              "    _:amit <friend> _:artyom .\n" +
              "\n" +
              "    _:luke <name> \"Luke\"@en .\n" +
              "    _:luke <name> \"Łukasz\"@pl .\n" +
              "    _:luke <age> \"77\" .\n" +
              "\n" +
              "    _:artyom <name> \"Артём\"@ru .\n" +
              "    _:artyom <name> \"Artyom\"@en .\n" +
              "    _:artyom <age> \"35\" .\n" +
              "\n" +
              "    _:sarah <name> \"Sarah\" .\n" +
              "    _:sarah <age> \"55\" .\n" +
              "\n" +
              "    _:sang <name> \"상현\"@ko .\n" +
              "    _:sang <name> \"Sang Hyun\"@en .\n" +
              "    _:sang <age> \"24\" .\n" +
              "    _:sang <friend> _:amit .\n" +
              "    _:sang <friend> _:catalina .\n" +
              "    _:sang <friend> _:hyung .\n" +
              "    _:sang <owns_pet> _:goldie .\n" +
              "\n" +
              "    _:hyung <name> \"형신\"@ko .\n" +
              "    _:hyung <name> \"Hyung Sin\"@en .\n" +
              "    _:hyung <friend> _:sang .\n" +
              "\n" +
              "    _:catalina <name> \"Catalina\" .\n" +
              "    _:catalina <age> \"19\" .\n" +
              "    _:catalina <friend> _:sang .\n" +
              "    _:catalina <owns_pet> _:perro .\n" +
              "\n" +
              "    _:rammy <name> \"Rammy the sheep\" .\n" +
              "\n" +
              "    _:goldie <name> \"Goldie\" .\n" +
              "\n" +
              "    _:perro <name> \"Perro\" .";

      // Run mutation
      Mutation mu = Mutation.newBuilder().setSetNquads(ByteString.copyFromUtf8(triples)).build();
      txn.mutate(mu);
      txn.commit();

    } finally {
      txn.discard();
    }
    // Query
    String query = "{\n" + "all(func: eq(name, \"Michael\")) {\n" + "    name\n" + "    age\n" + "    friend{name}\n" + "  }\n" + "}";
    Map<String, String> vars = Collections.singletonMap("$a", "Michael");
    Response res = dgraphClient.newTransaction().queryWithVars(query, vars);

    // Deserialize
    People ppl = gson.fromJson(res.getJson().toStringUtf8(), People.class);

    // Print results
    System.out.printf("people found: %d\n", ppl.all.size());
    ppl.all.forEach(person -> {
      System.out.println(person.name);
      System.out.println(person.age);
      person.friend.forEach(friend -> System.out.println(friend.name));
    });
  }

  static class Person {
    String name;
    int age;
    List<Person> friend;

    Person() {}
  }

  static class People {
    List<Person> all;

    People() {}
  }
}
