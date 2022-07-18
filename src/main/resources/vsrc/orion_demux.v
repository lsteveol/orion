`timescale 1ns/1ps

module orion_demux #(
  parameter WIDTH   = 1,
  parameter PA_INIT = 0,
  parameter PB_INIT = 0,
  parameter PC_INIT = 0
)(
  input  wire             reset,
  
  input  wire             inA_req,
  output wire             inA_ack,
  input  wire [WIDTH-1:0] inA_data,
  
  input  wire             inSel_req,
  output wire             inSel_ack,
  input  wire             inSel_data,   //just the sel line
  
  // B is selected with sel==1, C when sel==0
  output wire             outB_req,
  input  wire             outB_ack,
  output wire [WIDTH-1:0] outB_data,
  
  output wire             outC_req,
  input  wire             outC_ack,
  output wire [WIDTH-1:0] outC_data
);


reg   pa;
reg   pb;
reg   pc;

wire  click_req;
wire  click_ack;

assign inSel_ack  = pa;
assign inA_ack    = pa;
assign outB_req   = pb;
assign outB_data  = inA_data;
assign outC_req   = pc;
assign outC_data  = inA_data;

assign #10ps  click_req = (inSel_req & ~pa & inA_req) | (~inSel_req & pa & ~inA_req);

assign #10ps  click_ack = ~(outB_ack ^ pb) & ~(outC_ack ^ pc);

always @(posedge click_req or posedge reset) begin
  if(reset) begin
    pb  <= PB_INIT;
    pc  <= PC_INIT;
  end else begin
    pb  <= #10ps pb ^ inSel_data;
    pc  <= #10ps pc ^ ~inSel_data;
  end
end

always @(posedge click_ack or posedge reset) begin
  if(reset) begin
    pa  <= PA_INIT;
  end else begin
    pa  <= #10ps ~pa;
  end
end

endmodule
