package org.openapi2puml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.log4j.Log4j2;

@Controller
@Log4j2
public class Api {
	@GetMapping("/api/temp")
//	public File resourcesExample(@PathParam("path") String path) {
	public String resourcesExample() {
		log.info("Requested path: {}", "path");
		return "test";
	}

}
