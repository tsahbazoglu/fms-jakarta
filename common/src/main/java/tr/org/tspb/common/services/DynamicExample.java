package tr.org.tspb.common.services;

import htmlflow.HtmlFlow;
import htmlflow.HtmlView;
import java.util.Arrays;
import java.util.List;

class User {

    public final String name;
    public final List<String> Tasks;

    User(String name, List<String> tasks) {
        this.name = name;
        this.Tasks = tasks;
    }
}

public class DynamicExample {

    public static void main(String[] args) {
        User myUser = new User("Alice", Arrays.asList("Code", "Coffee", "Sleep"));

        // Define a view that expects a User object
        HtmlView<User> view = HtmlFlow.view(v -> v
                .html().
                body().
                h1().
                dynamic((el, model) -> {
                    if (model instanceof User user) {
                        el.text("Profile: " + user.name);
                    }
                }
                ).
                __().
                __().
                __().
                __());

        System.out.println(view.render(myUser));
    }
}
