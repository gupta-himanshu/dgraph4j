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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AppLink {
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
        String schema = "name: string @index(exact) .";
        Operation op = Operation.newBuilder().setSchema(schema).build();
        dgraphClient.alter(op);

        Gson gson = new Gson(); // For JSON encode/decode

        Transaction txn = dgraphClient.newTransaction();
        try {
            // Create data
            Person p1 = new Person();
            p1.name = "Alice";
            Person p2 = new Person();
            p2.name = "Bob";
            p1.friend = new ArrayList<>();
            p1.friend.add(p2);

            People ppl = new People();
            ppl.all = new ArrayList<>();
            ppl.all.add(p1);
            ppl.all.add(p2);

            // Serialize it
            String json = gson.toJson(ppl);

            // Run mutation
            Mutation mu =
                    Mutation.newBuilder().setSetJson(ByteString.copyFromUtf8(json)).build();
            txn.mutate(mu);
            txn.commit();

        } finally {
            txn.discard();
        }
        // Query
        String query =
                "query all($a: string){\n" + "all(func: eq(name, $a)) {\n" + "    name\n" + "    friend{name}\n" + "  }\n" + "}";
        Map<String, String> vars = Collections.singletonMap("$a", "Alice");
        Response res = dgraphClient.newTransaction().queryWithVars(query, vars);

        // Deserialize
        People ppl = gson.fromJson(res.getJson().toStringUtf8(), People.class);

        // Print results
        System.out.printf("people found: %d\n", ppl.all.size());
        ppl.all.forEach(person -> {
            System.out.println(person.name);
            person.friend.forEach(friend -> System.out.println(friend.name));
        });
    }

    static class Person {
        String name;
        //TODO: Try it without List
        List<Person> friend;

        Person() {}
    }

    static class People {
        List<Person> all;

        People() {}
    }
}
