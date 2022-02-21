module orion_reg_fork #(
  parameter WIDTH     = 1,
  parameter INIT      = 0,
  parameter PA_INIT   = 0,
  parameter PB_INIT   = 1,
  parameter PC_INIT   = 1
)(
  input  wire       reset,
  
  input  wire             inA_req,
  output wire             inA_ack,
  input  wire [WIDTH-1:0] inA_data,
  
  output wire             outB_req,
  input  wire             outB_ack,
  output wire [WIDTH-1:0] outB_data,
  
  output wire             outC_req,
  input  wire             outC_ack,
  output wire [WIDTH-1:0] outC_data
);

wire inA_token;
wire outB_bubble;
wire outC_bubble;
wire click;
reg  phase_a;
reg  phase_b;
reg  phase_c;
reg [WIDTH-1:0] data_reg;

assign #10ps inA_token    = inA_req ^ phase_a;
assign #10ps outB_bubble  = ~(phase_b ^ outB_ack);
assign #10ps outC_bubble  = ~(phase_c ^ outC_ack);

assign #15ps click        = inA_token & outB_bubble & outC_bubble;

assign inA_ack            = phase_a;

assign outB_req           = phase_b;
assign outB_data          = data_reg;

assign outC_req           = phase_c;
assign outC_data          = data_reg;


always @(posedge click or posedge reset) begin
  if(reset) begin
    phase_a   <= PA_INIT;
    phase_b   <= PB_INIT;
    phase_c   <= PC_INIT;
    data_reg  <= INIT;
  end else begin
    phase_a   <= #10ps ~phase_a;
    phase_b   <= #10ps ~phase_b;
    phase_c   <= #10ps ~phase_c;
    data_reg  <= #10ps inA_data;
  end
end

endmodule
