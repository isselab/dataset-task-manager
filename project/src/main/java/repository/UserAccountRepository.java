package repository;

import model.UserAccount;
import java.util.List;

public interface UserAccountRepository {
    List<UserAccount> load();
    void save(List<UserAccount> accounts);
}
