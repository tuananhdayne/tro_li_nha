package com.example.smarthomeui.test;

// Test file để kiểm tra Retrofit
public class RetrofitTest {
    public void testRetrofit() {
        try {
            // Test basic Retrofit classes
            retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl("https://test.com/")
                    .build();

            System.out.println("Retrofit test passed!");
        } catch (Exception e) {
            System.out.println("Retrofit test failed: " + e.getMessage());
        }
    }
}
