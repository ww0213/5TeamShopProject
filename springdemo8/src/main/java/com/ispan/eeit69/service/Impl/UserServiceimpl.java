package com.ispan.eeit69.service.Impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ispan.eeit69.dao.CustomRepository;
import com.ispan.eeit69.model.Role;
import com.ispan.eeit69.model.User;
import com.ispan.eeit69.repository.RoleRepository;
import com.ispan.eeit69.repository.UserRepository;
import com.ispan.eeit69.service.UserService;

@Service
@Transactional
public class UserServiceimpl implements UserService {

	private final UserRepository userrepository;
	private final BCryptPasswordEncoder passwordEncoder;
	private final RoleRepository roleRepository;
	private final CustomRepository customRepository;
	@Autowired
	private EmailService emailService;
	

	public UserServiceimpl(UserRepository userrepository, BCryptPasswordEncoder passwordEncoder,
	    RoleRepository roleRepository, CustomRepository customRepository) {
	    this.userrepository = userrepository;
	    this.passwordEncoder = passwordEncoder;
	    this.roleRepository = roleRepository;
	    this.customRepository = customRepository;
	}
	

	@Override
	public List<User> findAll() {

		return userrepository.findAll();
	}

	@Override
	public User findById(int theId) {

		User theUser = null;

		Optional<User> result = userrepository.findById(theId);

		if (result.isPresent()) {
			theUser = result.get();
		}

		return theUser;
	}

	@Override
	public void save(User theUser) {
		String encryptedPassword = passwordEncoder.encode(theUser.getPassword());

		theUser.setPassword(encryptedPassword);

		Role userRole = roleRepository.findByName("USER")
				.orElseThrow(() -> new RuntimeException("Error: Role is not found."));

		theUser.setRoles(new HashSet<Role>(Arrays.asList(userRole)));

		userrepository.save(theUser);
	}

	@Override
	public void deleteById(int theId) {
		userrepository.deleteById(theId);
	}

	@Override
	public User findByUserName(String userName) {

		return userrepository.findByUsername(userName);
	}

	@Override
	public User findByUserId(String username) {

		return userrepository.findByUsername(username);
	}

	@Override
	public void detach(User user) {

		customRepository.detach(user);
	}
	
	
	
	
	@Override
	public long getTotalUserCount() {
		return userrepository.count();
	}

	@Override
	public Page<User> getUserSubset(int startIndex, int recordsPerPage) {
	    int pageIndex = startIndex / recordsPerPage;
	    Pageable pageable = PageRequest.of(pageIndex, recordsPerPage);
	    return userrepository.findByUsernameContaining("", pageable);
	}

	@Override
	public long getTotalUserCountByKeyword(String keyword) {
		return userrepository.countByUsernameContaining(keyword);
	}
	

    @Override
    public Page<User> searchUsersByKeyword(String keyword, int startIndex, int recordsPerPage) {
        int pageIndex = startIndex / recordsPerPage;
        Page<User> users = userrepository.findByUsernameContaining(keyword, PageRequest.of(pageIndex, recordsPerPage));
        return users;
    }
	

	
	
    public String resetPassword(String email) throws EmailNotFoundException, MessagingException {
        User user = userrepository.findByEmail(email);
        if (user == null) {
            throw new EmailNotFoundException("找不到此電子郵件" + email);
        }

        String newPassword = RandomStringUtils.randomAlphanumeric(8);
        String encryptedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encryptedPassword);
        userrepository.save(user);

        emailService.sendNewPasswordEmail(user.getUsername(), newPassword, email);

        return newPassword;
    }

}