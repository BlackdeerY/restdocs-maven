package io.blackdeer.restdocsmaven;

import io.blackdeer.restdocsmaven.device.Device;
import io.blackdeer.restdocsmaven.device.DeviceRepository;
import io.blackdeer.restdocsmaven.hub.Hub;
import io.blackdeer.restdocsmaven.hub.HubRepository;
import io.blackdeer.restdocsmaven.user.User;
import io.blackdeer.restdocsmaven.user.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@SpringBootTest
//@AutoConfigureMockMvc    // .webAppContextSetup(webApplicationContext)
//@AutoConfigureRestDocs    // .apply(documentationConfiguration(restDocumentationContextProvider))
public class RestdocsTest {

    private MockMvc mockMvc;
    private RestDocumentationResultHandler restDocumentationResultHandler;

    private static HttpHeaders httpHeaders;
    private static List<HeaderDescriptor> headerDescriptorList;
    private static FieldDescriptor[] fieldDescriptorsUser;
    private static FieldDescriptor[] fieldDescriptorsHub;
    private static FieldDescriptor[] fieldDescriptorsDevice;

    /*
        여기에선 실제로 Spring Data JPA를 사용하지 않았다.
        실제로 repository들을 사용(save 등)하려면
        repository들을 MockBean이 아니라 실제로 사용하도록 하고,
        테스트 상황에서 재현하기 힘든 메서드(Service에 있게 되는)를 when으로 처리한다. (Service를 MockBean으로 한다)
        Controller는 Test Success의 의의를 위해 MockBean이 아닌, 실제 처리가 되도록 한다.
     */

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private HubRepository hubRepository;

    @MockBean
    private DeviceRepository deviceRepository;

    private static ArrayList<Principal> principals;
    private static ArrayList<User> users;
    private static ArrayList<Hub> hubs;
    private static ArrayList<Device> devices;

    @BeforeEach
    public void beforeEach(WebApplicationContext webApplicationContext,
                           RestDocumentationContextProvider restDocumentationContextProvider) {
        restDocumentationResultHandler =
                document("{method-name}",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()));
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(new CharacterEncodingFilter("UTF-8", true))
                .apply(documentationConfiguration(restDocumentationContextProvider)
                        .uris()
                        .withScheme("https")
                        .withHost("example.io")
                        .withPort(8084)
                )
                .alwaysDo(restDocumentationResultHandler)
                .build();
    }

    @BeforeAll
    public static void beforeAll() {
        users = new ArrayList<>(10);
        for (int i = 0; i < 10; ++i) {
            User user = new User(Long.valueOf(i + 1), String.format("사용자%02d", i + 1));
            users.add(user);
        }
        hubs = new ArrayList<>(20);
        devices = new ArrayList<>(100);
        for (int i = 0; i < 20; ++i) {
            User user = users.get(i / 2);
            Hub hub = new Hub(Long.valueOf(i + 1), String.format("%s의 %s허브", user.getName(), i % 2 == 0 ? "거실" : "별장"));
            int deviceCount = i % 2 + 2;
            for (int j = 0; j < deviceCount; ++j) {
                String deviceName = null;
                if (j == 0) {
                    deviceName = "침실전등";
                } else if (j == 1) {
                    deviceName = "화장실전등";
                } else {
                    deviceName = "차고전등";
                }
                Device device = new Device(Long.valueOf(devices.size() + 1), deviceName, hub);
                device.setHub(hub);
                hub.getDevices().add(device);
                devices.add(device);
            }
            hub.getUsers().add(user);
            user.getHubs().add(hub);
            hubs.add(hub);
        }

        httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer **********...");

        headerDescriptorList = new ArrayList<>(1);
        headerDescriptorList.add(headerWithName("Authorization").description("로그인한 계정의 권한(소유한 허브)과 일치하는 허브에만 접근 가능"));

        fieldDescriptorsUser = new FieldDescriptor[] {
                fieldWithPath("id")
                        .description("사용자 식별 ID"),
                fieldWithPath("name")
                        .description("사용자가 입력한 사용자 이름"),
                subsectionWithPath("hubs")
                        .description("사용자가 권한을 가진 허브들")
        };
        fieldDescriptorsHub = new FieldDescriptor[] {
                fieldWithPath("id")
                        .description("허브 식별 ID"),
                fieldWithPath("name")
                        .description("사용자가 지정한 허브의 이름"),
                subsectionWithPath("devices")
                        .description("허브에 연결된 장치들")
        };
        fieldDescriptorsDevice = new FieldDescriptor[] {
                fieldWithPath("id")
                        .description("장치 식별 ID"),
                fieldWithPath("name")
                        .description("사용자가 지정한 장치의 이름")
        };

        principals = new ArrayList<>(users.size());
        for (User user : users) {
            principals.add(new Principal() {
                @Override
                public String getName() {
                    return user.getName();
                }
            });
        }
    }

    @Test
    public void user_get_id() throws Exception {
        for (User user : users) {
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        }
        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/user/get/{id}",
                                        1L
                                )
                                .headers(httpHeaders)
//                                .principal(principals.get(0))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(restDocumentationResultHandler.
                        document(
                                requestHeaders(headerDescriptorList),
                                pathParameters(
                                        parameterWithName("id")
                                                .description("사용자 식별 ID")
                                ),
                                responseFields(fieldDescriptorsUser)
                        )
                );
    }

    @Test
    public void hub_get_id() throws Exception {
        for (Hub hub : hubs) {
            when(hubRepository.findById(hub.getId())).thenReturn(Optional.of(hub));
        }
        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/hub/get/{id}",
                                        1L
                                )
                                .headers(httpHeaders)
//                                .principal(principals.get(0))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(restDocumentationResultHandler.
                        document(
                                requestHeaders(headerDescriptorList),
                                pathParameters(
                                        parameterWithName("id")
                                                .description("허브 식별 ID")
                                ),
                                responseFields(fieldDescriptorsHub)
                        )
                );
    }

    @Test
    public void device_get_id() throws Exception {
        for (Device device : devices) {
            when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        }
        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/device/get/{id}",
                                        1L
                                )
                                .headers(httpHeaders)
//                                .principal(principals.get(0))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(restDocumentationResultHandler.
                        document(
                                requestHeaders(headerDescriptorList),
                                pathParameters(
                                        parameterWithName("id")
                                                .description("장치 식별 ID")
                                ),
                                responseFields(fieldDescriptorsDevice)
                        )
                );
    }

    @Test
    public void user_get_list() throws Exception {
        when(userRepository.findAll()).thenReturn(users);
        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/user/list"
                                )
                                .headers(httpHeaders)
//                                .principal(principals.get(0))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(restDocumentationResultHandler.
                        document(
                                requestHeaders(headerDescriptorList),
                                responseFields(
                                        fieldWithPath("[]")
                                                .description("모든 사용자")
                                ).andWithPrefix("[].", fieldDescriptorsUser)
                        )
                );
    }

    @Test
    public void hub_get_list() throws Exception {
        when(hubRepository.findAll()).thenReturn(hubs);
        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/hub/list"
                                )
                                .headers(httpHeaders)
//                                .principal(principals.get(0))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(restDocumentationResultHandler.
                        document(
                                requestHeaders(headerDescriptorList),
                                responseFields(
                                        fieldWithPath("[]")
                                                .description("모든 허브")
                                ).andWithPrefix("[].", fieldDescriptorsHub)
                        )
                );
    }

    @Test
    public void device_get_list() throws Exception {
        when(deviceRepository.findAll()).thenReturn(devices);
        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/device/list"
                                )
                                .headers(httpHeaders)
//                                .principal(principals.get(0))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(restDocumentationResultHandler.
                        document(
                                requestHeaders(headerDescriptorList),
                                responseFields(
                                        fieldWithPath("[]")
                                                .description("모든 장치")
                                ).andWithPrefix("[].", fieldDescriptorsDevice)
                        )
                );
    }

    @Test
    public void device_put_id() throws Exception {
        for (Device device : devices) {
            when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        }
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>(1);
        requestParams.add("name", "럭셔리전등");
        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .put(
                                        "/device/update/{id}",
                                        1L
                                )
                                .headers(httpHeaders)
//                                .principal(principals.get(0))
                                .params(requestParams)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(restDocumentationResultHandler.
                        document(
                                requestHeaders(headerDescriptorList),
                                pathParameters(
                                        parameterWithName("id")
                                                .description("장치 식별 ID")
                                ),
                                requestParameters(
                                        parameterWithName("name")
                                                .description("장치의 새로운 이름")
                                ),
                                responseFields(fieldDescriptorsDevice)
                        )
                );
    }

}
