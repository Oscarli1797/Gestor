package com.GestorProyectos.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.GestorProyectos.entity.User;
import com.GestorProyectos.repository.SavedCandidateRepository;
import com.GestorProyectos.repository.UserRepository;

/**
 * Enforces per-plan usage limits.
 *
 * Free plan:  200 searches / month, 30 saved candidates.
 * Pro  plan:  unlimited.
 */
@Service
public class QuotaService {

    static final int FREE_SEARCH_LIMIT    = 200;
    static final int FREE_CANDIDATE_LIMIT = 30;

    @Autowired private UserRepository           userRepo;
    @Autowired private SavedCandidateRepository candidateRepo;

    /**
     * Increments the search counter for this user.
     * Resets the counter if we are in a new calendar month.
     * Throws {@link QuotaExceededException} if a free-plan user has used up
     * their monthly allowance.
     */
    @Transactional
    public void checkAndIncrementSearch(String username) {
        User user = requireUser(username);
        if ("pro".equals(user.getPlan())) return;

        LocalDate today = LocalDate.now();
        LocalDate resetAt = user.getSearchResetAt();

        // Reset counter at the start of each new month
        if (resetAt == null || !YearMonth.from(today).equals(YearMonth.from(resetAt))) {
            user.setSearchCount(0);
            user.setSearchResetAt(today);
        }

        if (user.getSearchCount() >= FREE_SEARCH_LIMIT) {
            throw new QuotaExceededException(
                "Free plan limit reached: " + FREE_SEARCH_LIMIT
                + " searches per month. Upgrade to Pro for unlimited searches.");
        }

        user.setSearchCount(user.getSearchCount() + 1);
        userRepo.save(user);
    }

    /**
     * Throws {@link QuotaExceededException} if a free-plan user has already
     * saved the maximum number of candidates.
     */
    public void checkCandidateSave(String username) {
        User user = requireUser(username);
        if ("pro".equals(user.getPlan())) return;

        long count = candidateRepo.countByRecruiterId(user.getId());
        if (count >= FREE_CANDIDATE_LIMIT) {
            throw new QuotaExceededException(
                "Free plan limit reached: " + FREE_CANDIDATE_LIMIT
                + " saved candidates. Upgrade to Pro for unlimited candidates.");
        }
    }

    /** Returns current usage stats for the plan info endpoint. */
    public int currentSearchCount(User user) {
        LocalDate today = LocalDate.now();
        LocalDate resetAt = user.getSearchResetAt();
        if (resetAt == null || !YearMonth.from(today).equals(YearMonth.from(resetAt))) {
            return 0;
        }
        return user.getSearchCount();
    }

    private User requireUser(String username) {
        User user = userRepo.findByName(username);
        if (user == null) throw new IllegalStateException("User not found: " + username);
        return user;
    }

    // ── Inner exception ────────────────────────────────────────────────────

    public static class QuotaExceededException extends RuntimeException {
        public QuotaExceededException(String message) { super(message); }
    }
}
