package com.werdnadoow;


import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class MusicSearchApplicationTests {

    @Autowired
    private MockMvc mvc;
    
	@Test
	public void contextLoads() {
	}

    @Test
    public void getHello() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Greetings from Music Search!")));
        
    }
    
    @Test
    public void getSearch() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/search").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
        
    }

    @Test
    public void getSearchLove() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/search?terms=love*").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200));
        
    }
    @Test
    public void getSearchLoad() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/load?action=status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200));

    }
}