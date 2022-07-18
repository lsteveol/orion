module orion_mux #(
  parameter WIDTH     = 1,
  parameter PA_INIT   = 0,
  parameter PB_INIT   = 0,
  parameter PC_INIT   = 0,
  parameter PSEL_INIT = 0
)(
  input  wire             reset,
  
  input  wire             inA_req,
  output wire             inA_ack,
  input  wire [WIDTH-1:0] inA_data,
  
  input  wire             inB_req,
  output wire             inB_ack,
  input  wire [WIDTH-1:0] inB_data,
  
  input  wire             inSel_req,
  output wire             inSel_ack,
  input  wire             inSel_data,   //just the sel line
  
  output wire             out_req,
  input  wire             out_ack,
  output wire [WIDTH-1:0] out_data
);


wire  click_req;
wire  click_ack;
reg   phase_a, phase_b, phase_c, phase_sel;

wire  inA_token;
wire  inB_token;
wire  inSel_token;


assign #5ps  inA_token    = phase_a   ^ inA_req;
assign #5ps  inB_token    = phase_b   ^ inB_req;
assign #5ps  inSel_token  = phase_sel ^ inSel_req;


assign #10ps click_req = (inA_token & inSel_token & inSel_data) |
                         (inB_token & inSel_token & ~inSel_data);
                         
assign #10ps click_ack = ~(phase_c ^ out_ack);

assign out_data = inSel_data ? inA_data : inB_data;


always @(posedge click_req or posedge reset) begin
  if(reset) begin
    phase_c   <= PC_INIT;
  end else begin
    phase_c   <= #10ps ~phase_c;
  end
end

always @(posedge click_ack or posedge reset) begin
  if(reset) begin
    phase_a   <= PA_INIT;
    phase_b   <= PB_INIT;
    phase_sel <= PSEL_INIT;
  end else begin
    phase_a   <= #10ps phase_a ^ inSel_data;
    phase_b   <= #10ps phase_b ^ ~inSel_data;
    phase_sel <= #10ps inSel_req;
  end
end


endmodule
